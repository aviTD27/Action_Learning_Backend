package fr.epita.dto.Request;

import fr.epita.enums.CohortSeason;
import fr.epita.enums.CohortStatus;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;

@Data
public class CreateCohortRequest {

    /** Optional display name; auto-generated from season + year when blank (e.g. "Spring 2026"). */
    private String name;

    @NotNull
    private CohortSeason season;

    @NotNull
    private Integer academicYear;

    private CohortStatus status;

    /** Programmes that run in this intake (many-to-many). */
    private List<Long> programmeIds;
}
