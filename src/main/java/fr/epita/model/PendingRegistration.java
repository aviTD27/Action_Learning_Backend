package fr.epita.model;

import fr.epita.enums.RegistrationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "pending_registrations")
public class PendingRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orgName;

    private String country;
    private String websiteUrl;

    @Column(nullable = false)
    private String domain;

    private String adminFirstName;
    private String adminLastName;

    @Column(nullable = false)
    private String adminContactEmail;

    private String adminPhone;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus status;

    @Column(length = 2000)
    private String declineReason;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    private Instant reviewedAt;

    @PrePersist
    void onCreate() {
        if (submittedAt == null) submittedAt = Instant.now();
        if (status == null) status = RegistrationStatus.PENDING;
    }
}
