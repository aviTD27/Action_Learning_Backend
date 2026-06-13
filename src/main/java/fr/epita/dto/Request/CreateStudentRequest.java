package fr.epita.dto.Request;

import fr.epita.enums.StudentStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateStudentRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Student reference is required")
    private String studentRef;

    @NotNull(message = "Programme ID is required")
    private Long programmeId;

    @NotNull(message = "Status is required")
    private StudentStatus status;

    @NotNull(message = "Cohort ID is required")
    private Long cohortId;
}


