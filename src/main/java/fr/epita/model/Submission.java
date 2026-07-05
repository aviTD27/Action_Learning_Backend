package fr.epita.model;

import fr.epita.enums.SubmissionStatus;
import fr.epita.enums.SubmissionType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

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

    @Column(length = 4000)
    private String description;

    /** Free-text notes shown alongside the assignment. */
    @Column(length = 2000)
    private String additionalNotes;

    /** What the student must submit: FILE, TEXT or BOTH. */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20)")
    private SubmissionType submissionType = SubmissionType.BOTH;

    /** Lifecycle: DRAFT (hidden), PUBLISHED (visible), ARCHIVED. */
    @Enumerated(EnumType.STRING)
    @Column(columnDefinition = "VARCHAR(20)")
    private SubmissionStatus status = SubmissionStatus.DRAFT;

    /** Assignments now belong to a Course (not a cohort). */
    @ManyToOne
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @ManyToOne
    @JoinColumn(name = "lecturer_id")
    private Lecturer lecturer;

    @Column(nullable = false)
    private LocalDate dueDate;

    /** Time-of-day component of the deadline (defaults to 23:59). */
    private LocalTime dueTime;

    @Column(nullable = false)
    private int maxPoints;

    @Embedded
    private SubmissionRules rules;

    private String templateFileName;

    private String templateStoredPath;

    @Column(length = 5000)
    private String instructions;

    /** Student ids explicitly re-opened for a late exception. */
    @Builder.Default
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "submission_reopened_students",
            joinColumns = @JoinColumn(name = "submission_id"))
    @Column(name = "student_id")
    private Set<Long> reopenedStudentIds = new HashSet<>();

    private Instant lastNotifiedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = Instant.now();
        if (status == null) status = SubmissionStatus.DRAFT;
        if (submissionType == null) submissionType = SubmissionType.BOTH;
    }

    /** Full deadline as an Instant (UTC), using 23:59 when no time is set. */
    public java.time.LocalDateTime deadline() {
        return dueDate.atTime(dueTime != null ? dueTime : LocalTime.of(23, 59));
    }
}
