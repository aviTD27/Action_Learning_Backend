package fr.epita.repository;

import fr.epita.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    @Query(value = "SELECT * FROM students WHERE email = :email LIMIT 1", nativeQuery = true)
    Optional<Student> findByEmail(@Param("email") String email);

    @Query(value = "SELECT * FROM students WHERE LOWER(name) LIKE LOWER(CONCAT('%', :name, '%')) ORDER BY name ASC", nativeQuery = true)
    List<Student> findByName(@Param("name") String name);

    @Query(value = "SELECT * FROM students WHERE age = :age ORDER BY name ASC", nativeQuery = true)
    List<Student> findByAge(@Param("age") Integer age);
}
