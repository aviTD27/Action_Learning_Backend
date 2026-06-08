package fr.epita.controller;

import fr.epita.model.Student;
import fr.epita.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/students")
@CrossOrigin(origins = "*")
public class StudentController {

    @Autowired
    private StudentService studentService;


    @GetMapping
    public ResponseEntity<List<Student>> getAllStudents() {
        try {
            List<Student> students = studentService.getAll();
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Student> getStudentById(@PathVariable Long id) {
        try {
            Optional<Student> student = studentService.getById(id);
            return student.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<Student> createStudent(@RequestBody Student student) {
        try {
            Student createdStudent = studentService.create(student);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdStudent);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<Student> updateStudent(@PathVariable Long id, @RequestBody Student student) {
        try {
            Optional<Student> existingStudent = studentService.getById(id);
            if (existingStudent.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            student.setId(id);
            Student updatedStudent = studentService.update(student);
            return ResponseEntity.ok(updatedStudent);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteStudent(@PathVariable Long id) {
        try {
            if (studentService.getById(id).isPresent()) {
                studentService.delete(id);
                return ResponseEntity.noContent().build();
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/email/{email}")
    public ResponseEntity<Student> getStudentByEmail(@PathVariable String email) {
        try {
            Optional<Student> student = studentService.findByEmail(email);
            return student.map(ResponseEntity::ok)
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/name/{name}")
    public ResponseEntity<List<Student>> getStudentsByName(@PathVariable String name) {
        try {
            List<Student> students = studentService.findByName(name);
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/age/{age}")
    public ResponseEntity<List<Student>> getStudentsByAge(@PathVariable int age) {
        try {
            List<Student> students = studentService.findByAge(age);
            return ResponseEntity.ok(students);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
