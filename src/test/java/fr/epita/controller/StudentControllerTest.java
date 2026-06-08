package fr.epita.controller;

import fr.epita.model.Student;
import fr.epita.service.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StudentControllerTest {

    @Mock
    private StudentService studentService;

    @InjectMocks
    private StudentController studentController;

    private Student student;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        student = new Student("Jordy", "Meye", "jordy@outlook.com", 28, "789 Pine Road");
        student.setId(1L);
    }

    @Test
    public void testGetAllStudents_Success() {
        List<Student> students = Arrays.asList(student);
        when(studentService.getAll()).thenReturn(students);

        ResponseEntity<List<Student>> response = studentController.getAllStudents();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(studentService, times(1)).getAll();
    }

    @Test
    public void testGetAllStudents_Empty() {
        when(studentService.getAll()).thenReturn(Arrays.asList());

        ResponseEntity<List<Student>> response = studentController.getAllStudents();

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
        verify(studentService, times(1)).getAll();
    }

    @Test
    public void testGetStudentById_Found() {
        when(studentService.getById(1L)).thenReturn(Optional.of(student));

        ResponseEntity<Student> response = studentController.getStudentById(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Jordy", response.getBody().getName());
        verify(studentService, times(1)).getById(1L);
    }

    @Test
    public void testGetStudentById_NotFound() {
        when(studentService.getById(999L)).thenReturn(Optional.empty());

        ResponseEntity<Student> response = studentController.getStudentById(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(studentService, times(1)).getById(999L);
    }

    @Test
    public void testCreateStudent_Success() {
        Student newStudent = new Student("Jordy", "Meye", "jordy@outlook.com", 28, "789 Pine Road");
        when(studentService.create(any(Student.class))).thenReturn(newStudent);

        ResponseEntity<Student> response = studentController.createStudent(newStudent);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("Jordy", response.getBody().getName());
        verify(studentService, times(1)).create(any(Student.class));
    }

    @Test
    public void testUpdateStudent_Success() {
        student.setName("Updated Jordy");
        when(studentService.getById(1L)).thenReturn(Optional.of(student));
        when(studentService.update(any(Student.class))).thenReturn(student);

        ResponseEntity<Student> response = studentController.updateStudent(1L, student);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("Updated Jordy", response.getBody().getName());
        verify(studentService, times(1)).getById(1L);
        verify(studentService, times(1)).update(any(Student.class));
    }

    @Test
    public void testUpdateStudent_NotFound() {
        when(studentService.getById(999L)).thenReturn(Optional.empty());

        ResponseEntity<Student> response = studentController.updateStudent(999L, student);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(studentService, times(1)).getById(999L);
    }

    @Test
    public void testDeleteStudent_Success() {
        when(studentService.getById(1L)).thenReturn(Optional.of(student));

        ResponseEntity<Void> response = studentController.deleteStudent(1L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(studentService, times(1)).getById(1L);
        verify(studentService, times(1)).delete(1L);
    }

    @Test
    public void testDeleteStudent_NotFound() {
        when(studentService.getById(999L)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = studentController.deleteStudent(999L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(studentService, times(1)).getById(999L);
    }

    @Test
    public void testGetStudentByEmail_Found() {
        when(studentService.findByEmail("jordy@outlook.com")).thenReturn(Optional.of(student));

        ResponseEntity<Student> response = studentController.getStudentByEmail("jordy@outlook.com");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("jordy@outlook.com", response.getBody().getEmail());
        verify(studentService, times(1)).findByEmail("jordy@outlook.com");
    }

    @Test
    public void testGetStudentByEmail_NotFound() {
        when(studentService.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        ResponseEntity<Student> response = studentController.getStudentByEmail("nonexistent@example.com");

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(studentService, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    public void testGetStudentsByName_Found() {
        List<Student> students = Arrays.asList(student);
        when(studentService.findByName("Jordy")).thenReturn(students);

        ResponseEntity<List<Student>> response = studentController.getStudentsByName("Jordy");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(studentService, times(1)).findByName("Jordy");
    }

    @Test
    public void testGetStudentsByName_Empty() {
        when(studentService.findByName("NonExistent")).thenReturn(Arrays.asList());

        ResponseEntity<List<Student>> response = studentController.getStudentsByName("NonExistent");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
        verify(studentService, times(1)).findByName("NonExistent");
    }

    @Test
    public void testGetStudentsByAge_Found() {
        List<Student> students = Arrays.asList(student);
        when(studentService.findByAge(28)).thenReturn(students);

        ResponseEntity<List<Student>> response = studentController.getStudentsByAge(28);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().size());
        verify(studentService, times(1)).findByAge(28);
    }

    @Test
    public void testGetStudentsByAge_Empty() {
        when(studentService.findByAge(99)).thenReturn(Arrays.asList());

        ResponseEntity<List<Student>> response = studentController.getStudentsByAge(99);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(0, response.getBody().size());
        verify(studentService, times(1)).findByAge(99);
    }
}
