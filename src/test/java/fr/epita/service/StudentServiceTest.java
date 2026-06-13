package fr.epita.service;

import fr.epita.dto.Request.CreateStudentRequest;
import fr.epita.dto.Response.StudentResponse;
import fr.epita.enums.StudentStatus;
import fr.epita.model.Cohort;
import fr.epita.model.Programme;
import fr.epita.model.Student;
import fr.epita.repository.CohortRepository;
import fr.epita.repository.ProgrammeRepository;
import fr.epita.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StudentServiceTest {

    @Mock
    private StudentRepository studentRepo;

    @Mock
    private CohortRepository cohortRepo;

    @Mock
    private ProgrammeRepository programmeRepo;

    @InjectMocks
    private StudentService studentService;

    private CreateStudentRequest req;
    private Student student;
    private Cohort cohort;
    private Programme programme;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        programme = new Programme();
        programme.setId(1L);
        programme.setName("MSc SE");

        cohort = new Cohort();
        cohort.setId(10L);

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
                .email("alice.johnson@gmail.com")
                .password("Pass123")
                .studentRef("STU-2025F-001")
                .programme(programme)
                .status(StudentStatus.ACTIVE)
                .cohort(cohort)
                .build();
    }

    @Test
    void testCreateStudent() {
        when(studentRepo.existsByEmail(req.getEmail())).thenReturn(false);
        when(studentRepo.existsByStudentRef(req.getStudentRef())).thenReturn(false);
        when(cohortRepo.findById(10L)).thenReturn(Optional.of(cohort));
        when(programmeRepo.findById(1L)).thenReturn(Optional.of(programme));
        when(studentRepo.save(any(Student.class))).thenReturn(student);

        StudentResponse res = studentService.create(req);

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

        StudentResponse res = studentService.update(1L, req);

        assertEquals("Alice", res.getFirstName());
    }

    @Test
    void testDeactivateStudent() {
        when(studentRepo.findById(1L)).thenReturn(Optional.of(student));

        studentService.deactivate(1L);

        verify(studentRepo, times(1)).save(any(Student.class));
    }
}
