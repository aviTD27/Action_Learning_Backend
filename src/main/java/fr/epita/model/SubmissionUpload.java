package fr.epita.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "submission_uploads")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    private Submission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String storedPath;

    @Column(nullable = false)
    private String detectedFileType;

    private Long fileSizeBytes;

    private boolean compliancePassed;

    @Column(length = 5000)
    private String complianceReportJson;

    @Column(nullable = false)
    private Instant uploadedAt;

    private Boolean turnedIn;

    private Instant turnedInAt;
}
