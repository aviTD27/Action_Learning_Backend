package fr.epita.dto.Response;

import fr.epita.enums.ProgrammeStatus;
import lombok.Data;

import java.util.List;

@Data
public class ProgrammeResponse {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Long universityId;
    private String universityName;
    private ProgrammeStatus status;

    /** Cohorts (intakes) this programme is attached to. */
    private List<Long> cohortIds;
    private List<String> cohortNames;

    private int semesterCount;
}
