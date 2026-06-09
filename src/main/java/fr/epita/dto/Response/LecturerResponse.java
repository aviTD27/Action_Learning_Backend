package fr.epita.dto.Response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private Long programmeId;
    private String programmeName;
    private String status;
}
