package fr.epita.dto.Response;

import lombok.Data;

@Data
public class ProgrammeResponse {

    private Long id;
    private String name;
    private String code;
    private String description;
    private Long universityId;
    private String universityName;
}
