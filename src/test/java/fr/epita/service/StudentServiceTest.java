package fr.epita.service;

import fr.epita.model.Student;
import fr.epita.repository.StudentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;

    @InjectMocks
    private StudentService studentService;

    private Student student;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        student = new Student.Builder()
                .setName("Jordy")
                .setSurname("Meye")
                .setEmail("jordy@outlook.com")
                .setAge(28)
                .setAddress("789 Pine Road")
                .build();
    }

    @Test
    public void testCreateStudent() {
        when(studentRepository.save(student)).thenReturn(student);
        
        Student createdStudent = studentService.create(student);
        
        assertNotNull(createdStudent);
        assertEquals("Jordy", createdStudent.getName());
        verify(studentRepository, times(1)).save(student);
    }

    @Test
    public void testGetStudentById() {
        Long id = 1L;
        student.setId(id);
        when(studentRepository.findById(id)).thenReturn(Optional.of(student));
        
        Optional<Student> foundStudent = studentService.getById(id);
        
        assertTrue(foundStudent.isPresent());
        assertEquals("Jordy", foundStudent.get().getName());
        verify(studentRepository, times(1)).findById(id);
    }

    @Test
    public void testGetAllStudents() {
        List<Student> students = Arrays.asList(student);
        when(studentRepository.findAll()).thenReturn(students);
        
        List<Student> result = studentService.getAll();
        
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(studentRepository, times(1)).findAll();
    }

    @Test
    public void testDeleteStudent() {
        Long id = 1L;
        studentService.delete(id);
        
        verify(studentRepository, times(1)).deleteById(id);
    }

    @Test
    public void testFindByEmail() {
        when(studentRepository.findByEmail("jordy@outlook.com")).thenReturn(Optional.of(student));
        
        Optional<Student> foundStudent = studentService.findByEmail("jordy@outlook.com");
        
        assertTrue(foundStudent.isPresent());
        assertEquals("jordy@outlook.com", foundStudent.get().getEmail());
    }

    @Test
    public void testFindByName() {
        List<Student> students = Arrays.asList(student);
        when(studentRepository.findByName("Jordy")).thenReturn(students);
        
        List<Student> result = studentService.findByName("Jordy");
        
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(studentRepository, times(1)).findByName("Jordy");
    }

    @Test
    public void testFindByAge() {
        List<Student> students = Arrays.asList(student);
        when(studentRepository.findByAge(28)).thenReturn(students);
        
        List<Student> result = studentService.findByAge(28);
        
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(studentRepository, times(1)).findByAge(28);
    }

    @Test
    public void testUpdateStudent() {
        student.setId(1L);
        when(studentRepository.save(student)).thenReturn(student);
        
        Student updatedStudent = studentService.update(student);
        
        assertNotNull(updatedStudent);
        assertEquals("Jordy", updatedStudent.getName());
        verify(studentRepository, times(1)).save(student);
    }

    @Test
    public void testUpdateStudentWithoutId() {
        assertThrows(IllegalArgumentException.class, () -> {
            studentService.update(student);
        });
    }

    @Test
    public void testDeleteAll() {
        studentService.deleteAll();
        verify(studentRepository, times(1)).deleteAll();
    }}

