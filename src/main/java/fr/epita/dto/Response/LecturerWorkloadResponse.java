package fr.epita.dto.Response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LecturerWorkloadResponse {
    private Long lecturerId;
    private String lecturerName;
    private long assignments;
    private long cohorts;
    /** Turned-in submissions on this lecturer's assignments still awaiting a released grade. */
    private long gradingBacklog;
}
