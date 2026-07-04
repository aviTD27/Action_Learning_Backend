package fr.epita.dto.Request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class CreateCourseRequest {

    @NotBlank
    private String name;

    private String code;

    private String description;

    @NotNull
    private Long semesterId;

    /** Optional teaching lecturer. */
    private Long lecturerId;
}
