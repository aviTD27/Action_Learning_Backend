package fr.epita.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmissionRules {

    private String allowedFileTypes;

    private int maxAttempts;

    private boolean lateAllowed;

    private Integer minWordCount;

    private Integer maxWordCount;

    /** Maximum allowed upload size in bytes; null = no explicit limit. */
    private Long maxFileSizeBytes;

    private String namingPattern;

    @Column(length = 1000)
    private String requiredHeadings;
}
