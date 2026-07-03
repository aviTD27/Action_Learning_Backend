package fr.epita.model;

import fr.epita.enums.AnnouncementAudience;
import fr.epita.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "announcements")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false, length = 2000)
    private String message;

    /** Display name of the sender (e.g. "Sarah Duboix") */
    @Column(nullable = false)
    private String senderName;

    /** Role of the sender: ROLE_UNI_ADMIN or ROLE_LECTURER */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role senderRole;

    /** University this announcement belongs to — used for scoping */
    @Column(nullable = false)
    private Long universityId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnouncementAudience audience;

    @Column(nullable = false, updatable = false)
    private Instant sentAt;

    @PrePersist
    void onCreate() {
        if (sentAt == null) sentAt = Instant.now();
    }
}
