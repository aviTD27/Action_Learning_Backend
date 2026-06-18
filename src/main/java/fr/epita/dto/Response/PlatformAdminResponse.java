package fr.epita.dto.Response;

import fr.epita.enums.Role;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class PlatformAdminResponse {
    private Long id;
    private String firstName;
    private String surname;
    private String email;
    private Role role;
    private boolean blocked;
    private boolean deleted;
    private LocalDate dateOfBirth;
}
