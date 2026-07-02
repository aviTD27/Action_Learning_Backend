package fr.epita.dto.Request;

import fr.epita.enums.CohortStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateCohortRequest {

    @NotBlank
    private String name;

    @NotNull
    private Long programmeId;

    private CohortStatus status;

    private List<Long> lecturerIds;
}

