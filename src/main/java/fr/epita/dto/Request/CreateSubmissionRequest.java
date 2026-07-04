package fr.epita.dto.Request;

import fr.epita.enums.SubmissionStatus;
import fr.epita.enums.SubmissionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class CreateSubmissionRequest {

    @NotBlank
    private String title;

    private String description;

    /** Free-text notes shown alongside the assignment. */
    private String additionalNotes;

    /** Single course the assignment belongs to. */
    private Long courseId;

    /** One or more courses to assign to; when set, one assignment is created per course. */
    private List<Long> courseIds;

    private Long lecturerId;

    @NotNull
    private LocalDate dueDate;

    /** Optional time-of-day for the deadline (defaults to 23:59). */
    private LocalTime dueTime;

    /** FILE, TEXT or BOTH. Defaults to BOTH. */
    private SubmissionType submissionType;

    /** DRAFT or PUBLISHED. Defaults to DRAFT (save as draft). */
    private SubmissionStatus status;

    @Min(1)
    private int maxPoints;

    @NotNull
    @Valid
    private SubmissionRulesRequest rules;

    private String templateFileName;

    private String instructions;
}
