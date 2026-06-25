package fr.epita.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrendPointResponse {
    private String month;       // short label, e.g. "Jan 2026"
    private long submissions;   // assignments created in that month
    private double avgScore;    // mean released-grade percentage for grades graded that month; 0 if none
}
