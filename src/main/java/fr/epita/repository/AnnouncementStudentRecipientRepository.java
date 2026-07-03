package fr.epita.repository;

import fr.epita.model.AnnouncementStudentRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementStudentRecipientRepository extends JpaRepository<AnnouncementStudentRecipient, Long> {
    List<AnnouncementStudentRecipient> findByStudentIdOrderByAnnouncement_SentAtDesc(Long studentId);
    List<AnnouncementStudentRecipient> findByStudentIdAndReadFlagFalse(Long studentId);
}
