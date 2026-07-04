package fr.epita.dto.Response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SessionStudentResponse {
    private Long studentId;
    private String firstName;
    private String lastName;
    private String studentRef;
    private Long recordId;
    private String status;
    private Integer minutesLate;
}
