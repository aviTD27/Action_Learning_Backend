package fr.epita.dto.Response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortResponse {
    private Long id;
    private String name;
    private Long programmeId;
    private String programmeName;
    private String status;
}

