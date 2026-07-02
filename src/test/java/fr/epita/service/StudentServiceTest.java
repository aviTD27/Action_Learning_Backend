package fr.epita.service;

import fr.epita.dto.Request.CreateStudentRequest;
import fr.epita.dto.Response.StudentResponse;
import fr.epita.enums.StudentStatus;
import fr.epita.model.AppUser;
import fr.epita.model.Cohort;
import fr.epita.model.Programme;
import fr.epita.model.Student;
import fr.epita.model.University;
import fr.epita.repository.AppUserRepository;
import fr.epita.repository.CohortRepository;
import fr.epita.repository.PendingRegistrationRepository;
import fr.epita.repository.ProgrammeRepository;
import fr.epita.repository.StudentRepository;
import fr.epita.repository.UniversityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class StudentServiceTest {

    @Mock private StudentRepository studentRepo;
    @Mock private CohortRepository cohortRepo;
    @Mock private ProgrammeRepository programmeRepo;
    @Mock private AppUserRepository appUserRepo;
    @Mock private UniversityRepository universityRepo;
    @Mock private PendingRegistrationRepository registrationRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private StudentService studentService;

    private CreateStudentRequest req;
    private Student student;
    private Cohort cohort;
    private Programme programme;
    private University university;
    private AppUser currentUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        university = University.builder().id(1L).name("EPITA").code("EPITA").domain("epita.fr").build();

        programme = new Programme();
        programme.setId(1L);
        programme.setName("MSc SE");
        programme.setUniversity(university);

        cohort = new Cohort();
        cohort.setId(10L);
        cohort.setName("SE 2025");

        // Logged-in uni-admin in the same tenant as the student.
        currentUser = AppUser.builder().universityId(1L).email("admin@epita.fr").build();

        req = new CreateStudentRequest();
        req.setFirstName("Alice");
        req.setLastName("Johnson");
        req.setEmail("alice.johnson@gmail.com");
        req.setPassword("Pass123");
        req.setStudentRef("STU-2025F-001");
        req.setProgrammeId(1L);
        req.setStatus(StudentStatus.ACTIVE);
        req.setCohortId(10L);

        student = Student.builder()
                .id(1L)
                .firstName("Alice")
                .lastName("Johnson")
                .email("johnson-alice@epita.fr")
                .password("hashed")
                .studentRef("STU-2025F-001")
                .programme(programme)
                .status(StudentStatus.ACTIVE)
                .cohort(cohort)
                .build();
    }

    @Test
    void testCreateStudent() {
        when(studentRepo.existsByStudentRef(req.getStudentRef())).thenReturn(false);
        when(cohortRepo.findById(10L)).thenReturn(Optional.of(cohort));
        when(programmeRepo.findById(1L)).thenReturn(Optional.of(programme));
        when(universityRepo.findById(1L)).thenReturn(Optional.of(university));
        when(studentRepo.existsByEmail(anyString())).thenReturn(false);
        when(appUserRepo.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(studentRepo.save(any(Student.class))).thenReturn(student);

        StudentResponse res = studentService.create(req, 1L);

        assertEquals("Alice", res.getFirstName());
        assertEquals(1L, res.getProgrammeId());
        verify(studentRepo, times(1)).save(any(Student.class));
    }

    @Test
    void testGetAllStudents() {
        when(studentRepo.findAll()).thenReturn(List.of(student));

        List<StudentResponse> res = studentService.getAll(null);

        assertEquals(1, res.size());
    }

    @Test
    void testUpdateStudent() {
        when(studentRepo.findById(1L)).thenReturn(Optional.of(student));
        when(programmeRepo.findById(1L)).thenReturn(Optional.of(programme));
        when(cohortRepo.findById(10L)).thenReturn(Optional.of(cohort));
        when(studentRepo.save(any(Student.class))).thenReturn(student);

        StudentResponse res = studentService.update(1L, req, currentUser);

        assertEquals("Alice", res.getFirstName());
    }

    @Test
    void testDeactivateStudent() {
        when(studentRepo.findById(1L)).thenReturn(Optional.of(student));
        when(studentRepo.save(any(Student.class))).thenReturn(student);

        studentService.deactivate(1L, currentUser);

        verify(studentRepo, times(1)).save(any(Student.class));
    }
}
