package fr.epita.dto.Response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class AttendanceRecordResponse {
    private Long recordId;
    private Long sessionId;
    private LocalDate sessionDate;
    private String topic;
    private String cohortName;
    private Long studentId;
    private String studentName;
    private String studentRef;
    private String status;
    private Integer minutesLate;
    private Instant markedAt;
}
