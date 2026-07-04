package fr.epita.dto.Response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class StudentAttendanceStatsResponse {
    private Long studentId;
    private String studentName;
    private String studentRef;
    private List<CohortStats> cohorts;

    @Data
    @Builder
    public static class CohortStats {
        private Long cohortId;
        private String cohortName;
        private String programmeName;
        private int totalSessions;
        private int present;
        private int late;
        private int absent;
        private double attendanceRate;
        private boolean qualifiedForExam;
    }
}
