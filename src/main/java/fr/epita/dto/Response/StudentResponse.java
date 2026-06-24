package fr.epita.dto.Response;

import fr.epita.enums.StudentStatus;
import lombok.Data;

@Data
public class StudentResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String studentRef;
    private Long programmeId;
    private String programmeName;
    private StudentStatus status;
    private Long cohortId;
    private String cohortName;
    private String universityName;
}


