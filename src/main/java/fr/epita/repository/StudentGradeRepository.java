package fr.epita.repository;

import fr.epita.model.StudentGrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudentGradeRepository extends JpaRepository<StudentGrade, Long> {
    List<StudentGrade> findBySubmissionId(Long submissionId);
    Optional<StudentGrade> findBySubmissionIdAndStudentId(Long submissionId, Long studentId);
    void deleteBySubmissionId(Long submissionId);
}
