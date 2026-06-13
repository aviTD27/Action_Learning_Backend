package fr.epita.dto.Request;

import fr.epita.enums.CohortStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateCohortRequest {

    @NotBlank
    private String name;

    @NotNull
    private Long programmeId;

    private CohortStatus status;
}

