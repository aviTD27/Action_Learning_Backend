package fr.epita.dto.Request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateSubmissionRequest {

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Long cohortId;

    private Long lecturerId;

    @NotNull
    private LocalDate dueDate;

    @Min(1)
    private int maxPoints;

    @NotNull
    @Valid
    private SubmissionRulesRequest rules;

    private String templateFileName;

    private String instructions;
}
