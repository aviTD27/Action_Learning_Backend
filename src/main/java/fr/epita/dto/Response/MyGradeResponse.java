package fr.epita.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyGradeResponse {
    private Long submissionId;
    private String submissionTitle;
    private int maxPoints;
    private double grade;
    private String feedback;
    private Instant gradedAt;
    private Instant releasedAt;
    private boolean revised;
}
