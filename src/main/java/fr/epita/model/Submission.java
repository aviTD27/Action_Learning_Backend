package fr.epita.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "submissions")
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @ManyToOne
    @JoinColumn(name = "cohort_id", nullable = false)
    private Cohort cohort;

    @ManyToOne
    @JoinColumn(name = "lecturer_id")
    private Lecturer lecturer;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private int maxPoints;

    @Embedded
    private SubmissionRules rules;

    private String templateFileName;

    private String templateStoredPath;

    @Column(length = 5000)
    private String instructions;

    // TODO Email Notif
    private Instant lastNotifiedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
