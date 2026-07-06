package fr.epita.service;

import fr.epita.dto.Request.CreateStudentRequest;
import fr.epita.dto.Response.MyCohortResponse;
import fr.epita.dto.Response.StudentResponse;
import fr.epita.enums.RegistrationStatus;
import fr.epita.enums.Role;
import fr.epita.enums.StudentStatus;
import fr.epita.model.AppUser;
import fr.epita.model.Cohort;
import fr.epita.model.Programme;
import fr.epita.model.Student;
import fr.epita.model.University;
import fr.epita.repository.AppUserRepository;
import fr.epita.repository.CohortRepository;
import fr.epita.repository.LecturerRepository;
import fr.epita.repository.PendingRegistrationRepository;
import fr.epita.repository.ProgrammeRepository;
import fr.epita.repository.StudentRepository;
import fr.epita.repository.UniversityRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class StudentService {

    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$!";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StudentRepository studentRepository;
    private final CohortRepository cohortRepository;
    private final ProgrammeRepository programmeRepository;
    private final AppUserRepository appUserRepository;
    private final LecturerRepository lecturerRepository;
    private final UniversityRepository universityRepository;
    private final PendingRegistrationRepository registrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public List<StudentResponse> getAll(Long universityId) {
        List<Student> students = (universityId != null)
                ? studentRepository.findByProgramme_UniversityId(universityId)
                : studentRepository.findAll();
        return students.stream()
                .map(this::toResponse)
                .toList();
    }

    public List<StudentResponse> getByCohort(Long cohortId) {
        return studentRepository.findByCohortId(cohortId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public StudentResponse create(CreateStudentRequest request, Long universityId) {

        String studentRef = generateStudentRef();

        Cohort cohort = cohortRepository.findById(request.getCohortId())
                .orElseThrow(() -> new EntityNotFoundException("Cohort not found"));

        Programme programme = programmeRepository.findById(request.getProgrammeId())
                .orElseThrow(() -> new EntityNotFoundException("Programme not found"));

        String domain = resolveDomain(universityId, programme);

        String platformEmail = generatePlatformEmail(request.getFirstName(), request.getLastName(), domain);

        String tempPassword = generateTempPassword();
        String hashed = passwordEncoder.encode(tempPassword);

        AppUser login = AppUser.builder()
                .firstName(request.getFirstName())
                .surname(request.getLastName())
                .email(platformEmail)
                .password(hashed)
                .role(Role.ROLE_STUDENT)
                .universityId(universityId)
                .build();
        appUserRepository.save(login);

        Student student = Student.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(platformEmail)
                .password(hashed)
                .studentRef(studentRef)
                .programme(programme)
                .status(request.getStatus())
                .cohort(cohort)
                .build();
        Student saved = studentRepository.save(student);
        // Apply the login block at creation too (e.g. a student created as SUSPENDED cannot log in).
        syncLoginBlocked(saved);
        StudentResponse response = toResponse(saved);

        String recipient = (request.getPersonalEmail() != null && !request.getPersonalEmail().isBlank())
                ? request.getPersonalEmail()
                : platformEmail;
        emailService.sendAccountCreatedEmail(recipient, request.getFirstName(), platformEmail, tempPassword, "Student");

        return response;
    }

    /**
     * Resolves the university email domain.
     * Sources tried in order: university.domain → pending_registration → admin user email.
     * Every resolved value is sanitized: if it contains '@' (i.e. a full email was stored by mistake),
     * only the part after the last '@' is kept so we always return a bare domain like "uct.ac.za".
     */
    private String resolveDomain(Long universityId, Programme programme) {
        University university = null;

        if (universityId != null) {
            university = universityRepository.findById(universityId).orElse(null);
        }
        if (university == null && programme.getUniversity() != null) {
            university = programme.getUniversity();
        }

        if (university != null && university.getDomain() != null && !university.getDomain().isBlank()) {
            return extractDomain(university.getDomain());
        }

        if (university != null) {
            String domainFromReg = registrationRepository
                    .findFirstByOrgNameIgnoreCaseAndStatus(university.getName(), RegistrationStatus.APPROVED)
                    .map(r -> r.getDomain())
                    .orElse(null);
            if (domainFromReg != null && !domainFromReg.isBlank()) {
                return extractDomain(domainFromReg);
            }
        }

        // Level 3: every university admin's email is firstname.lastname@domain — extract the suffix.
        if (universityId != null) {
            Optional<AppUser> admin = appUserRepository.findFirstByUniversityIdAndRole(universityId, Role.ROLE_UNI_ADMIN);
            if (admin.isPresent()) {
                String result = extractDomain(admin.get().getEmail());
                if (!result.isBlank()) return result;
            }
        }

        throw new IllegalStateException(
                "University domain is not configured. Please ensure the university was registered through the platform.");
    }

    /**
     * Extracts the bare domain from a value that may be either "uct.ac.za" or a full email "esther.smith@uct.ac.za".
     * Returns only the part after the last '@', trimmed and lowercased.
     */
    private String extractDomain(String raw) {
        if (raw == null) return "";
        raw = raw.trim().toLowerCase();
        int atIdx = raw.lastIndexOf('@');
        return atIdx >= 0 ? raw.substring(atIdx + 1) : raw;
    }

    private String generatePlatformEmail(String firstName, String lastName, String domain) {
        // If a full email was stored as the domain (e.g. "esther.smith@uct.ac.za"), keep only the part after @
        if (domain.contains("@")) {
            domain = domain.substring(domain.lastIndexOf('@') + 1);
        }
        String base = firstName.toLowerCase().replaceAll("[^a-z0-9]", "")
                + "."
                + lastName.toLowerCase().replaceAll("[^a-z0-9]", "");
        String email = base + "@" + domain;
        int suffix = 2;
        while (studentRepository.existsByEmail(email) || appUserRepository.existsByEmail(email)) {
            email = base + suffix + "@" + domain;
            suffix++;
        }
        return email;
    }

    private String generateStudentRef() {
        String ref;
        do {
            StringBuilder sb = new StringBuilder("STU-");
            for (int i = 0; i < 8; i++) sb.append(RANDOM.nextInt(10));
            ref = sb.toString();
        } while (studentRepository.existsByStudentRef(ref));
        return ref;
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    @Transactional
    public StudentResponse update(Long id, CreateStudentRequest request, AppUser currentUser) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        validateUniversityAccess(student.getProgramme().getUniversity().getId(), currentUser);

        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            student.setEmail(request.getEmail());
        }
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            student.setPassword(request.getPassword());
        }
        student.setStatus(request.getStatus());

        Programme programme = programmeRepository.findById(request.getProgrammeId())
                .orElseThrow(() -> new EntityNotFoundException("Programme not found"));
        student.setProgramme(programme);

        if (request.getCohortId() != null) {
            Cohort cohort = cohortRepository.findById(request.getCohortId())
                    .orElseThrow(() -> new RuntimeException("Cohort not found"));
            student.setCohort(cohort);
        }

        Student saved = studentRepository.save(student);
        syncLoginBlocked(saved);
        return toResponse(saved);
    }

    @Transactional
    public void deactivate(Long id, AppUser currentUser) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        validateUniversityAccess(student.getProgramme().getUniversity().getId(), currentUser);
        student.setStatus(StudentStatus.INACTIVE);
        syncLoginBlocked(studentRepository.save(student));
    }

    /** A suspended / inactive / expelled / dropped-out student cannot log in. */
    private void syncLoginBlocked(Student student) {
        StudentStatus st = student.getStatus();
        boolean blocked = st == StudentStatus.SUSPENDED
                || st == StudentStatus.INACTIVE
                || st == StudentStatus.EXPELLED
                || st == StudentStatus.DROPPED_OUT;
        appUserRepository.findByEmail(student.getEmail()).ifPresent(u -> {
            u.setBlocked(blocked);
            appUserRepository.save(u);
        });
    }

    private void validateUniversityAccess(Long resourceUniversityId, AppUser currentUser) {
        if (currentUser == null || currentUser.getUniversityId() == null) return;
        if (!currentUser.getUniversityId().equals(resourceUniversityId)) {
            throw new AccessDeniedException("Access denied: resource belongs to a different university");
        }
    }

    public StudentResponse getMyProfile(String email) {
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found"));
        return toResponse(student);
    }

    public MyCohortResponse getMyCohort(String email) {
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        Cohort cohort = student.getCohort();
        if (cohort == null) throw new IllegalStateException("Student is not assigned to a cohort");

        Programme programme = student.getProgramme();
        if (programme == null) throw new IllegalStateException("Student is not enrolled in a programme");

        List<String> lecturerNames = lecturerRepository.findByProgrammes_Id(programme.getId())
                .stream()
                .map(l -> l.getFirstName() + " " + l.getLastName())
                .sorted()
                .toList();

        return MyCohortResponse.builder()
                .cohortId(cohort.getId())
                .cohortName(cohort.getName())
                .programmeName(programme.getName())
                .programmeCode(programme.getCode())
                .status(cohort.getStatus().name())
                .universityName(programme.getUniversity() != null
                        ? programme.getUniversity().getName() : null)
                .lecturerNames(lecturerNames)
                .build();
    }

    private StudentResponse toResponse(Student student) {
        StudentResponse studentResponse = new StudentResponse();
        studentResponse.setId(student.getId());
        studentResponse.setFirstName(student.getFirstName());
        studentResponse.setLastName(student.getLastName());
        studentResponse.setEmail(student.getEmail());
        studentResponse.setStudentRef(student.getStudentRef());
        studentResponse.setProgrammeId(student.getProgramme().getId());
        studentResponse.setProgrammeName(student.getProgramme().getName());
        studentResponse.setStatus(student.getStatus());
        studentResponse.setCohortId(student.getCohort() != null ? student.getCohort().getId() : null);
        studentResponse.setCohortName(student.getCohort() != null ? student.getCohort().getName() : null);
        studentResponse.setUniversityName(
                student.getProgramme().getUniversity() != null
                        ? student.getProgramme().getUniversity().getName()
                        : null
        );
        return studentResponse;
    }
}
