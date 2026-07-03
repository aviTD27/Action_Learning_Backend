package fr.epita.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionResponse {
    private Long id;
    private String title;
    private String description;
    private Long cohortId;
    private String cohortName;
    private Long lecturerId;
    private LocalDate dueDate;
    private int maxPoints;
    private String allowedFileTypes;
    private int maxAttempts;
    private boolean lateAllowed;
    private Integer minWordCount;
    private Integer maxWordCount;
    private String namingPattern;
    private String requiredHeadings;
    private String templateFileName;
    private boolean hasTemplate;
    private String instructions;
    private Instant lastNotifiedAt;
    private Instant createdAt;
}
