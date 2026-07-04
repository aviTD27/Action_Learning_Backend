package fr.epita.dto.Request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateSemesterRequest {

    @NotBlank
    private String name;

    private Integer orderIndex;

    @NotNull
    private Long programmeId;
}
