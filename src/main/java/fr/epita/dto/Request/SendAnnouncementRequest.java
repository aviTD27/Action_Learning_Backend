package fr.epita.dto.Request;

import fr.epita.enums.AnnouncementAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SendAnnouncementRequest {

    @NotBlank
    private String subject;

    @NotBlank
    private String message;

    @NotNull
    private AnnouncementAudience audience;

    /** Required when audience = ALL_COHORT_STUDENTS */
    private Long cohortId;

    /** Required when audience = SPECIFIC_STUDENTS */
    private List<Long> studentIds;

    /** Required when audience = SPECIFIC_LECTURERS */
    private List<Long> lecturerIds;
}
