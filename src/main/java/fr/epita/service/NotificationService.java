package fr.epita.service;

import fr.epita.dto.Response.NotificationResponse;
import fr.epita.enums.NotificationType;
import fr.epita.model.Notification;
import fr.epita.model.Student;
import fr.epita.model.Submission;
import fr.epita.repository.NotificationRepository;
import fr.epita.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final StudentRepository studentRepository;
    private final EmailService emailService;

    @Transactional
    public void notifyCohort(Submission submission, NotificationType type, String message) {
        List<Student> students = studentRepository.findByCohortId(submission.getCohort().getId());

        // 1. In-platform notification for each student.
        List<Notification> notifications = students.stream()
                .map(student -> Notification.builder()
                        .student(student)
                        .submission(submission)
                        .type(type)
                        .message(message)
                        .readFlag(false)
                        .build())
                .toList();
        notificationRepository.saveAll(notifications);

        // 2. Email each student 
        String subject = "Action Learning Platform — " + submission.getTitle();
        for (Student student : students) {
            if (student.getEmail() != null && !student.getEmail().isBlank()) {
                emailService.sendNotificationEmail(student.getEmail(), student.getFirstName(), subject, message);
            }
        }
    }

    public List<NotificationResponse> getForStudent(Long studentId) {
        return notificationRepository.findByStudentIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public long unreadCount(Long studentId) {
        return notificationRepository.findByStudentIdAndReadFlagFalse(studentId).size();
    }

    @Transactional
    public NotificationResponse markRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Notification not found"));
        notification.setReadFlag(true);
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead(Long studentId) {
        List<Notification> unread = notificationRepository.findByStudentIdAndReadFlagFalse(studentId);
        unread.forEach(n -> n.setReadFlag(true));
        notificationRepository.saveAll(unread);
    }

    private NotificationResponse toResponse(Notification n) {
        return NotificationResponse.builder()
                .id(n.getId())
                .submissionId(n.getSubmission() != null ? n.getSubmission().getId() : null)
                .submissionTitle(n.getSubmission() != null ? n.getSubmission().getTitle() : null)
                .type(n.getType().name())
                .message(n.getMessage())
                .read(n.isReadFlag())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
