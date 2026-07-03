package fr.epita.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AtRiskStudentResponse {
    private Long studentId;
    private String studentName;
    private String studentRef;
    private String cohortName;
    private String programmeName;
    /** Average released-grade percentage; null when the student has no grades yet. */
    private Double avgScorePct;
    private long gradedCount;
    /** Published assignments in the student's cohort whose deadline passed and were not submitted. */
    private long missedSubmissions;
    private String reason;
}
