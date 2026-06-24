package fr.epita.service;

import fr.epita.dto.Request.CreateStudentRequest;
import fr.epita.dto.Response.StudentResponse;
import fr.epita.enums.Role;
import fr.epita.enums.StudentStatus;
import fr.epita.model.AppUser;
import fr.epita.model.Cohort;
import fr.epita.model.Programme;
import fr.epita.model.Student;
import fr.epita.repository.AppUserRepository;
import fr.epita.repository.CohortRepository;
import fr.epita.repository.ProgrammeRepository;
import fr.epita.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$!";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final StudentRepository studentRepository;
    private final CohortRepository cohortRepository;
    private final ProgrammeRepository programmeRepository;
    private final AppUserRepository appUserRepository;
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

        String email = request.getEmail().trim().toLowerCase();

        if (studentRepository.existsByEmail(email))
            throw new IllegalStateException("Email already exists");

        if (studentRepository.existsByStudentRef(request.getStudentRef()))
            throw new IllegalStateException("Student reference already exists");

        if (appUserRepository.existsByEmail(email))
            throw new IllegalStateException("An account with this email already exists");

        Cohort cohort = cohortRepository.findById(request.getCohortId())
                .orElseThrow(() -> new EntityNotFoundException("Cohort not found"));

        Programme programme = programmeRepository.findById(request.getProgrammeId())
                .orElseThrow(() -> new EntityNotFoundException("Programme not found"));

        String tempPassword = generateTempPassword();
        String hashed = passwordEncoder.encode(tempPassword);

        AppUser login = AppUser.builder()
                .firstName(request.getFirstName())
                .surname(request.getLastName())
                .email(email)
                .password(hashed)
                .role(Role.ROLE_STUDENT)
                .universityId(universityId)
                .build();
        appUserRepository.save(login);

        Student student = Student.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(email)
                .password(hashed)
                .studentRef(request.getStudentRef())
                .programme(programme)
                .status(request.getStatus())
                .cohort(cohort)
                .build();
        StudentResponse response = toResponse(studentRepository.save(student));

        emailService.sendAccountCreatedEmail(email, request.getFirstName(), email, tempPassword, "Student");

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
    public StudentResponse update(Long id, CreateStudentRequest request) {

        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        student.setFirstName(request.getFirstName());
        student.setLastName(request.getLastName());
        student.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            student.setPassword(request.getPassword());
        }
        student.setStudentRef(request.getStudentRef());
        student.setStatus(request.getStatus());

        Programme programme = programmeRepository.findById(request.getProgrammeId())
                .orElseThrow(() -> new EntityNotFoundException("Programme not found"));
        student.setProgramme(programme);

        if (request.getCohortId() != null) {
            Cohort cohort = cohortRepository.findById(request.getCohortId())
                    .orElseThrow(() -> new RuntimeException("Cohort not found"));
            student.setCohort(cohort);
        }

        return toResponse(studentRepository.save(student));
    }

    @Transactional
    public void deactivate(Long id) {
        Student student = studentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        student.setStatus(StudentStatus.INACTIVE);
        studentRepository.save(student);
    }

    public StudentResponse getMyProfile(String email) {
        Student student = studentRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found"));
        return toResponse(student);
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
