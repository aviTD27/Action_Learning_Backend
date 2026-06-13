package fr.epita.dto.Request;

import fr.epita.enums.LecturerStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateLecturerRequest {

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String lecturerRef;

    @NotEmpty
    private List<Long> programmeIds;

    private String password;

    private LecturerStatus status;
}
