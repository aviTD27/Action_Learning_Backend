package fr.epita.repository;

import fr.epita.enums.StudentStatus;
import fr.epita.model.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Long> {

    boolean existsByEmail(String email);

    boolean existsByStudentRef(String studentRef);

    List<Student> findByCohortId(Long cohortId);
    List<Student> findByProgramme_UniversityId(Long universityId);
    List<Student> findByStatus(StudentStatus status);
    Optional<Student> findByEmail(String email);

}
