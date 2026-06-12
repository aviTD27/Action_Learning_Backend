package fr.epita.dto.Request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateProgrammeRequest {

    private String name;
    private String code;
    private String description;

    @NotNull
    private Long universityId;
}
