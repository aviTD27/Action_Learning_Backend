package fr.epita.dto.Response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemesterResponse {
    private Long id;
    private String name;
    private int orderIndex;
    private Long programmeId;
    private String programmeName;
    private int courseCount;
}
