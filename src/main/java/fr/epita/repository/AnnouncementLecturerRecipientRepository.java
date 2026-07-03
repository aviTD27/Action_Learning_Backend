package fr.epita.repository;

import fr.epita.model.AnnouncementLecturerRecipient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementLecturerRecipientRepository extends JpaRepository<AnnouncementLecturerRecipient, Long> {
    List<AnnouncementLecturerRecipient> findByLecturerIdOrderByAnnouncement_SentAtDesc(Long lecturerId);
    List<AnnouncementLecturerRecipient> findByLecturerIdAndReadFlagFalse(Long lecturerId);
}
