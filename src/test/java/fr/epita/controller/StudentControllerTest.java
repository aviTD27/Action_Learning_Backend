package fr.epita.controller;

import fr.epita.dto.Request.CreateStudentRequest;
import fr.epita.dto.Response.StudentResponse;
import fr.epita.enums.StudentStatus;
import fr.epita.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StudentControllerTest {

    @Mock
    private StudentService studentService;

    @InjectMocks
    private StudentController studentController;

    private CreateStudentRequest req;
    private StudentResponse res;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        req = new CreateStudentRequest();
        req.setFirstName("Alice");
        req.setLastName("Johnson");
        req.setEmail("alice.johnson@gmail.com");
        req.setPassword("Pass123");
        req.setStudentRef("STU-2025F-001");
        req.setProgrammeId(1L);
        req.setStatus(StudentStatus.ACTIVE);
        req.setCohortId(10L);

        res = new StudentResponse();
        res.setId(1L);
        res.setFirstName("Alice");
        res.setLastName("Johnson");
        res.setEmail("alice.johnson@gmail.com");
        res.setStudentRef("STU-2025F-001");
        res.setProgrammeId(1L);
        res.setProgrammeName("MSc SE");
        res.setStatus(StudentStatus.ACTIVE);
        res.setCohortId(10L);
    }

    @Test
    void testCreateStudent() {
        when(studentService.create(req)).thenReturn(res);

        ResponseEntity<StudentResponse> response = studentController.create(req);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Alice", response.getBody().getFirstName());
        verify(studentService, times(1)).create(req);
    }

    @Test
    void testGetAllStudents() {
        when(studentService.getAll(null)).thenReturn(List.of(res));

        ResponseEntity<List<StudentResponse>> response = studentController.getAll(null);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(studentService, times(1)).getAll(null);
    }

    @Test
    void testGetByCohort() {
        when(studentService.getByCohort(10L)).thenReturn(List.of(res));

        ResponseEntity<List<StudentResponse>> response = studentController.getByCohort(10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(studentService, times(1)).getByCohort(10L);
    }

    @Test
    void testUpdateStudent() {
        when(studentService.update(1L, req)).thenReturn(res);

        ResponseEntity<StudentResponse> response = studentController.update(1L, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Alice", response.getBody().getFirstName());
        verify(studentService, times(1)).update(1L, req);
    }

    @Test
    void testDeactivateStudent() {
        doNothing().when(studentService).deactivate(1L);

        ResponseEntity<Void> response = studentController.deactivate(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(studentService, times(1)).deactivate(1L);
    }
}
