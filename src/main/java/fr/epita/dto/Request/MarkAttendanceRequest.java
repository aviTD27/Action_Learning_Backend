package fr.epita.dto.Request;

import fr.epita.enums.AttendanceStatus;
import lombok.Data;

@Data
public class MarkAttendanceRequest {
    private Long studentId;
    private AttendanceStatus status;
    private Integer minutesLate;
}
