package fr.epita.service;

import fr.epita.model.Student;
import fr.epita.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    public Student create(Student student) {
        return studentRepository.save(student);
    }

    public Optional<Student> getById(Long id) {
        return studentRepository.findById(id);
    }

    public List<Student> getAll() {
        return studentRepository.findAll();
    }

    public Student update(Student student) {
        if (student.getId() == null || student.getId() <= 0) {
            throw new IllegalArgumentException("Cannot update student without valid ID");
        }
        return studentRepository.save(student);
    }

    public void delete(Long id) {
        studentRepository.deleteById(id);
    }

    public void deleteAll() {
        studentRepository.deleteAll();
    }

    public Optional<Student> findByEmail(String email) {
        return studentRepository.findByEmail(email);
    }

    public List<Student> findByName(String name) {
        return studentRepository.findByName(name);
    }

    public List<Student> findByAge(int age) {
        return studentRepository.findByAge(age);
    }
}
