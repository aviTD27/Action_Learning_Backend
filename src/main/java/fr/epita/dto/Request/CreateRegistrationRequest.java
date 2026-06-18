package fr.epita.dto.Request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRegistrationRequest {

    @NotBlank
    private String orgName;

    private String country;
    private String websiteUrl;

    @NotBlank
    private String domain;

    private String adminFirstName;
    private String adminLastName;

    @NotBlank
    @Email
    private String adminContactEmail;

    private String adminPhone;
    private String description;
}
