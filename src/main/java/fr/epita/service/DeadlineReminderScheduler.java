package fr.epita.service;

import fr.epita.enums.NotificationType;
import fr.epita.model.Submission;
import fr.epita.repository.NotificationRepository;
import fr.epita.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DeadlineReminderScheduler {

    private static final ZoneId ZONE = ZoneId.of("Europe/Paris");
    private static final LocalTime DEADLINE_TIME = LocalTime.of(23, 59);

    private static final Map<NotificationType, Duration> WINDOWS = Map.of(
            NotificationType.REMINDER_24H, Duration.ofHours(24),
            NotificationType.REMINDER_12H, Duration.ofHours(12),
            NotificationType.REMINDER_1H, Duration.ofHours(1)
    );

    private final SubmissionRepository submissionRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Scheduled(fixedRate = 5 * 60 * 1000)
    public void sendDeadlineReminders() {
        Instant now = Instant.now();
        List<Submission> submissions = submissionRepository.findAll();

        for (Submission submission : submissions) {
            Instant deadline = submission.getDueDate().atTime(DEADLINE_TIME).atZone(ZONE).toInstant();
            if (deadline.isBefore(now)) continue; // deadline passed

            for (Map.Entry<NotificationType, Duration> window : WINDOWS.entrySet()) {
                NotificationType type = window.getKey();
                Duration remaining = Duration.between(now, deadline);

                if (remaining.compareTo(window.getValue()) <= 0
                        && !notificationRepository.existsBySubmissionIdAndType(submission.getId(), type)) {
                    notificationService.notifyCohort(submission, type, reminderMessage(submission, type));
                }
            }
        }
    }

    private String reminderMessage(Submission s, NotificationType type) {
        String left = switch (type) {
            case REMINDER_24H -> "24 hours";
            case REMINDER_12H -> "12 hours";
            case REMINDER_1H -> "1 hour";
            default -> "";
        };
        return "Reminder: \"" + s.getTitle() + "\" is due in less than " + left
                + " (deadline " + s.getDueDate() + " 23:59).";
    }
}
