package fr.epita.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerOverviewResponse {

    /** Turned-in submissions with no grade yet (across all this lecturer's assignments). */
    private long gradingBacklog;

    private long compliancePassed;
    private long complianceFailed;
    private long onTime;
    private long late;

    private List<NeedsGradingItem> needsGrading;
    private List<AtRiskStudentResponse> atRisk;
    private List<GradeDistributionResponse> gradeDistribution;
    private List<ActivityItem> recentActivity;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NeedsGradingItem {
        private Long submissionId;
        private String title;
        private String cohortName;
        private long awaiting;
        private String oldestSubmittedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        /** SUBMISSION or GRADE. */
        private String type;
        private String text;
        private String at;
    }
}
