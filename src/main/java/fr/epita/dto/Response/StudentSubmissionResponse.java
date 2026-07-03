package fr.epita.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentSubmissionResponse {
    private Long studentId;
    private String studentName;
    private String studentRef;
    private String studentEmail;
    /** SUBMITTED, LATE or NOT_SUBMITTED. */
    private String status;
    private Long uploadId;
    private String fileName;
    private String submittedAt;
    private int attemptNumber;
    private boolean late;
    private boolean reopened;
}
