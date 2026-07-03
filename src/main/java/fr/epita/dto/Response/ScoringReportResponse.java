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
public class ScoringReportResponse {

    private double overallScore;
    private String level;
    private int wordCount;
    private boolean embeddingsComputed;
    private boolean requiresHumanReview;
    private List<CriterionScoreResponse> criteria;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CriterionScoreResponse {
        private String label;
        private double score;
        private String feedback;
        private String confidence;
        private boolean requiresReview;
    }
}
