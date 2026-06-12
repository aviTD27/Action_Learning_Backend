package fr.epita.dto.Request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUniversityRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String code;
}
