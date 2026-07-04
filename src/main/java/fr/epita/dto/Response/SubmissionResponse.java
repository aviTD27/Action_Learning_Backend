package fr.epita.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponse {
    private Long id;
    private String title;
    private String description;
    private String additionalNotes;
    private String submissionType;
    private String status;
    private Long courseId;
    private String courseName;
    private Long programmeId;
    private String programmeName;
    private Long lecturerId;
    private LocalDate dueDate;
    private LocalTime dueTime;
    private int maxPoints;
    private String allowedFileTypes;
    private int maxAttempts;
    private boolean lateAllowed;
    private Integer minWordCount;
    private Integer maxWordCount;
    private Long maxFileSizeBytes;
    private String namingPattern;
    private String requiredHeadings;
    private String templateFileName;
    private boolean hasTemplate;
    private boolean hasTemplateFile;
    private String instructions;
    private Instant lastNotifiedAt;
    private Instant createdAt;
}
