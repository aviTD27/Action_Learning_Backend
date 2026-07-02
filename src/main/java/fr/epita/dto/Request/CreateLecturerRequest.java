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
    private String email;

    @NotBlank
    private String lecturerRef;

    @NotEmpty
    private List<Long> programmeIds;

    /** Optional contact phone number. */
    private String phone;

    private String password;

    private LecturerStatus status;
}
