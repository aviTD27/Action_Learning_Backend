package fr.epita.controller;

import fr.epita.dto.Request.CreateStudentRequest;
import fr.epita.dto.Response.StudentResponse;
import fr.epita.enums.StudentStatus;
import fr.epita.model.AppUser;
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
    private AppUser currentUser;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // The logged-in uni-admin whose tenant the student belongs to.
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

        res = new StudentResponse();
        res.setId(1L);
        res.setFirstName("Alice");
        res.setLastName("Johnson");
        res.setEmail("johnson-alice@epita.fr");
        res.setStudentRef("STU-2025F-001");
        res.setProgrammeId(1L);
        res.setProgrammeName("MSc SE");
        res.setStatus(StudentStatus.ACTIVE);
        res.setCohortId(10L);
    }

    @Test
    void testCreateStudent() {
        when(studentService.create(req, 1L)).thenReturn(res);

        ResponseEntity<StudentResponse> response = studentController.create(req, currentUser);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Alice", response.getBody().getFirstName());
        verify(studentService, times(1)).create(req, 1L);
    }

    @Test
    void testGetAllStudents() {
        // No explicit universityId → controller falls back to the current user's tenant (1L).
        when(studentService.getAll(1L)).thenReturn(List.of(res));

        ResponseEntity<List<StudentResponse>> response = studentController.getAll(null, currentUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(studentService, times(1)).getAll(1L);
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
        when(studentService.update(1L, req, currentUser)).thenReturn(res);

        ResponseEntity<StudentResponse> response = studentController.update(1L, req, currentUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Alice", response.getBody().getFirstName());
        verify(studentService, times(1)).update(1L, req, currentUser);
    }

    @Test
    void testDeactivateStudent() {
        doNothing().when(studentService).deactivate(1L, currentUser);

        ResponseEntity<Void> response = studentController.deactivate(1L, currentUser);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(studentService, times(1)).deactivate(1L, currentUser);
    }
}
