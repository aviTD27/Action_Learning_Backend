package fr.epita.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantSummaryResponse {
    private long totalStudents;
    private long activeStudents;
    private long totalLecturers;
    private long activeLecturers;
    private long totalProgrammes;
    private long totalCohorts;
    private long activeCohorts;

    private long totalSubmissions;   // assignments created for this tenant's cohorts
    private long releasedGrades;     // StudentGrade rows with status RELEASED
    private long gradedThisMonth;    // released grades whose gradedAt falls in the current month

    private double avgScorePct;      // mean of (grade / maxPoints * 100) over released grades; 0 if none
}
