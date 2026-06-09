package fr.epita.dto.Request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateLecturerRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Email
    @NotBlank
    private String email;

    @NotNull
    private Long programmeId;
}
