package fr.epita.dto.Request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SubmissionRulesRequest {

    private String allowedFileTypes;

    @Min(1)
    private int maxAttempts;

    private boolean lateAllowed;

    private Integer minWordCount;

    private Integer maxWordCount;

    private String namingPattern;

    private String requiredHeadings;
}
