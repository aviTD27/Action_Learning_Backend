package fr.epita.dto.Response;

import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
@Builder
public class TimetableResponse {

    private Long id;
    private String title;
    private String room;
    private DayOfWeek dayOfWeek;
    private LocalTime startTime;
    private LocalTime endTime;
    private String color;

    private Long cohortId;
    private String cohortName;

    private Long lecturerId;
    private String lecturerName;

    private Long universityId;
    private String universityName;
}
