package fr.epita.repository;

import fr.epita.enums.NotificationType;
import fr.epita.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<Notification> findByStudentIdAndReadFlagFalse(Long studentId);
    boolean existsBySubmissionIdAndType(Long submissionId, NotificationType type);
    void deleteBySubmissionId(Long submissionId);
}
