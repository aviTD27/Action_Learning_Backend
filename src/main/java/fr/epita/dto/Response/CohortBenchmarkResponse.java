package fr.epita.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortBenchmarkResponse {
    private Long cohortId;
    private String cohortName;
    private String programmeName;
    private long students;
    private long submissions;    
    private long releasedGrades;  
    private double avgScorePct;   
    private int rank;             
}
