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
public class RegistrationResponse {
    private Long id;
    private String orgName;
    private String country;
    private String websiteUrl;
    private String domain;
    private String adminFirstName;
    private String adminLastName;
    private String adminContactEmail;
    private String adminPhone;
    private String description;
    private String status;
    private String declineReason;
    private Instant submittedAt;
    private Instant reviewedAt;
}
