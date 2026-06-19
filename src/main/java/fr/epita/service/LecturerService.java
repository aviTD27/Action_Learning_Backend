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
    public LecturerResponse create(CreateLecturerRequest request) {
        if (lecturerRepository.existsByLecturerRef(request.getLecturerRef())) {
            throw new IllegalStateException("Lecturer reference already exists");
        }
        String email = request.getEmail().trim().toLowerCase();
        if (appUserRepository.existsByEmail(email)) {
            throw new IllegalStateException("An account with this email already exists");
        }
        List<Programme> programmes = resolveProgrammes(request.getProgrammeIds());

        String tempPassword = generateTempPassword();
        String hashed = passwordEncoder.encode(tempPassword);

        AppUser login = AppUser.builder()
                .firstName(request.getFirstName())
                .surname(request.getLastName())
                .email(email)
                .password(hashed)
                .role(Role.ROLE_LECTURER)
                .build();
        appUserRepository.save(login);

        Lecturer lecturer = Lecturer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(email)
                .lecturerRef(request.getLecturerRef())
                .password(hashed)
                .programmes(programmes)
                .status(LecturerStatus.ACTIVE)
                .build();
        LecturerResponse response = toResponse(lecturerRepository.save(lecturer));

        emailService.sendAccountCreatedEmail(email, request.getFirstName(), email, tempPassword, "Lecturer");

        return response;
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    @Transactional
    public LecturerResponse update(Long id, CreateLecturerRequest request) {
        Lecturer lecturer = lecturerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lecturer not found"));

        List<Programme> programmes = resolveProgrammes(request.getProgrammeIds());

        lecturer.setFirstName(request.getFirstName());
        lecturer.setLastName(request.getLastName());
        lecturer.setEmail(request.getEmail());
        lecturer.setLecturerRef(request.getLecturerRef());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            lecturer.setPassword(request.getPassword());
        }
        lecturer.setProgrammes(programmes);
        if (request.getStatus() != null) {
            lecturer.setStatus(request.getStatus());
        }

        return toResponse(lecturerRepository.save(lecturer));
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
                .programmeIds(lecturer.getProgrammes().stream().map(Programme::getId).toList())
                .programmeNames(lecturer.getProgrammes().stream().map(Programme::getName).toList())
                .status(lecturer.getStatus().name())
                .build();
    }
}
