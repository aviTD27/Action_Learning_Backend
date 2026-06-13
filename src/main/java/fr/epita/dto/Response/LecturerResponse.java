package fr.epita.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String lecturerRef;
    private List<Long> programmeIds;
    private List<String> programmeNames;
    private String status;
}
