package fr.epita.dto.Request;

import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateAttendanceSessionRequest {
    private Long cohortId;
    private LocalDate sessionDate;
    private String topic;
}
