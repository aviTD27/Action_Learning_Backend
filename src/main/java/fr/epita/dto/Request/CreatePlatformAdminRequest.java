package fr.epita.dto.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePlatformAdminRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String surname;

    @NotBlank
    @Email
    private String email;
}
