package fr.epita.dto.Request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GradeRequest {

    @NotNull
    private Double grade;

    private String feedback;
}
