package fr.epita.dto.Response;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class AttendanceSessionResponse {
    private Long id;
    private Long cohortId;
    private String cohortName;
    private Long lecturerId;
    private String lecturerName;
    private LocalDate sessionDate;
    private String topic;
    private Instant createdAt;
    private int totalStudents;
    private int markedStudents;
}
