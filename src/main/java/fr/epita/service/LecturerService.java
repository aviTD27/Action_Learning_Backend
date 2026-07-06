package fr.epita.service;

import fr.epita.dto.Request.CreateLecturerRequest;
import fr.epita.dto.Response.LecturerResponse;
import fr.epita.enums.LecturerStatus;
import fr.epita.enums.Role;
import fr.epita.model.AppUser;
import fr.epita.model.Lecturer;
import fr.epita.model.Programme;
import fr.epita.repository.AppUserRepository;
import fr.epita.repository.LecturerRepository;
import fr.epita.repository.ProgrammeRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LecturerService {

    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$!";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final LecturerRepository lecturerRepository;
    private final ProgrammeRepository programmeRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public List<LecturerResponse> getAll(Long universityId) {
        List<Lecturer> lecturers = (universityId != null)
                ? lecturerRepository.findDistinctByProgrammes_UniversityId(universityId)
                : lecturerRepository.findAll();
        return lecturers.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LecturerResponse create(CreateLecturerRequest request, Long universityId) {
        String lecturerRef = generateLecturerRef();
        // The form email (if provided) is the PERSONAL email — only the recipient of the credentials.
        String personalEmail = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : null;
        List<Programme> programmes = resolveProgrammes(request.getProgrammeIds());

        // Professional login email: surname-firstname@<university-domain>, numeric suffix on collision.
        String domain = resolveDomain(programmes);
        String professionalEmail = generateProfessionalEmail(request.getLastName(), request.getFirstName(), domain);

        String tempPassword = generateTempPassword();
        String hashed = passwordEncoder.encode(tempPassword);

        AppUser login = AppUser.builder()
                .firstName(request.getFirstName())
                .surname(request.getLastName())
                .email(professionalEmail)
                .password(hashed)
                .role(Role.ROLE_LECTURER)
                .universityId(universityId)
                .build();
        appUserRepository.save(login);

        Lecturer lecturer = Lecturer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(professionalEmail)
                .phone(request.getPhone())
                .lecturerRef(lecturerRef)
                .password(hashed)
                .programmes(programmes)
                .status(LecturerStatus.ACTIVE)
                .build();
        LecturerResponse response = toResponse(lecturerRepository.save(lecturer));

        String recipient = (personalEmail != null && !personalEmail.isBlank()) ? personalEmail : professionalEmail;
        emailService.sendAccountCreatedEmail(recipient, request.getFirstName(), professionalEmail, tempPassword, "Lecturer");

        return response;
    }

    private String generateLecturerRef() {
        String ref;
        do {
            StringBuilder sb = new StringBuilder("LEC-");
            for (int i = 0; i < 8; i++) sb.append(RANDOM.nextInt(10));
            ref = sb.toString();
        } while (lecturerRepository.existsByLecturerRef(ref));
        return ref;
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    /** surname-firstname@domain, lowercased; appends a numeric suffix on collision. */
    private String generateProfessionalEmail(String surname, String firstName, String domain) {
        String local = sanitize(surname) + "-" + sanitize(firstName);
        String candidate = local + "@" + domain;
        if (!appUserRepository.existsByEmail(candidate)) return candidate;
        int n = 2;
        while (appUserRepository.existsByEmail(local + n + "@" + domain)) n++;
        return local + n + "@" + domain;
    }

    private String sanitize(String s) {
        return s == null ? "" : s.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /** University email domain from the lecturer's programmes; falls back to a code-based domain. */
    private String resolveDomain(List<Programme> programmes) {
        return programmes.stream()
                .map(Programme::getUniversity)
                .filter(u -> u != null)
                .map(u -> {
                    String d = u.getDomain();
                    if (d != null && !d.isBlank()) return d.trim().toLowerCase();
                    String code = u.getCode();
                    return code != null ? code.trim().toLowerCase().replaceAll("[^a-z0-9]", "") + ".edu" : "university.edu";
                })
                .findFirst()
                .orElse("university.edu");
    }

    @Transactional
    public LecturerResponse update(Long id, CreateLecturerRequest request, AppUser currentUser) {
        Lecturer lecturer = lecturerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lecturer not found"));

        validateUniversityAccess(lecturer, currentUser);

        List<Programme> programmes = resolveProgrammes(request.getProgrammeIds());

        lecturer.setFirstName(request.getFirstName());
        lecturer.setLastName(request.getLastName());
        // The professional login email and lecturer ref are fixed at creation and must never
        // change on edit (login identity / stable reference). Only name and phone are editable.
        if (request.getPhone() != null) {
            lecturer.setPhone(request.getPhone());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            lecturer.setPassword(request.getPassword());
        }
        lecturer.setProgrammes(programmes);
        if (request.getStatus() != null) {
            lecturer.setStatus(request.getStatus());
        }

        Lecturer saved = lecturerRepository.save(lecturer);
        // Keep the login in sync: a deactivated lecturer cannot log in.
        appUserRepository.findByEmail(saved.getEmail()).ifPresent(u -> {
            u.setBlocked(saved.getStatus() != LecturerStatus.ACTIVE);
            appUserRepository.save(u);
        });
        return toResponse(saved);
    }

    private List<Programme> resolveProgrammes(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new EntityNotFoundException("At least one programme is required");
        }
        List<Programme> programmes = programmeRepository.findAllById(ids);
        if (programmes.size() != ids.size()) {
            throw new EntityNotFoundException("Programme not found");
        }
        return programmes;
    }

    private LecturerResponse toResponse(Lecturer lecturer) {
        return LecturerResponse.builder()
                .id(lecturer.getId())
                .firstName(lecturer.getFirstName())
                .lastName(lecturer.getLastName())
                .email(lecturer.getEmail())
                .lecturerRef(lecturer.getLecturerRef())
                .phone(lecturer.getPhone())
                .programmeIds(lecturer.getProgrammes().stream().map(Programme::getId).toList())
                .programmeNames(lecturer.getProgrammes().stream().map(Programme::getName).toList())
                .status(lecturer.getStatus().name())
                .build();
    }

    private void validateUniversityAccess(Lecturer lecturer, AppUser currentUser) {
        if (currentUser == null || currentUser.getUniversityId() == null) return;
        boolean belongs = lecturer.getProgrammes().stream()
                .anyMatch(p -> currentUser.getUniversityId().equals(p.getUniversity().getId()));
        if (!belongs) {
            throw new AccessDeniedException("Access denied: resource belongs to a different university");
        }
    }
}
