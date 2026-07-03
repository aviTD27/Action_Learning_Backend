package fr.epita.repository;

import fr.epita.model.SubmissionUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubmissionUploadRepository extends JpaRepository<SubmissionUpload, Long> {

    List<SubmissionUpload> findBySubmissionIdAndStudentIdOrderByUploadedAtDesc(Long submissionId, Long studentId);

    List<SubmissionUpload> findBySubmissionIdAndTurnedInTrue(Long submissionId);

    Optional<SubmissionUpload> findTopBySubmissionIdAndStudentIdOrderByUploadedAtDesc(Long submissionId, Long studentId);

    long countBySubmissionIdAndStudentId(Long submissionId, Long studentId);

    long countBySubmissionId(Long submissionId);
}
