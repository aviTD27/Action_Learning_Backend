package fr.epita.dto.Response;

import lombok.*;

import java.util.List;

/** A cohort is now an intake season (e.g. "Spring 2026"), university-wide, with attached programmes. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CohortResponse {
    private Long id;
    private String name;
    private String season;          // SPRING / FALL
    private int academicYear;
    private Long universityId;
    private String universityName;
    private String status;
    private List<Long> programmeIds;
    private List<String> programmeNames;
    private int studentCount;
}
