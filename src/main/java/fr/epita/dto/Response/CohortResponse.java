package fr.epita.dto.Response;

import lombok.*;

import java.util.List;

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
    private List<Long> lecturerIds;
    private List<String> lecturerNames;
}

