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
public class GradeResponse {
    private Long studentId;
    private String studentName;
    private String studentRef;
    private double grade;
    private String feedback;
    private String status;
    private Instant gradedAt;
}
