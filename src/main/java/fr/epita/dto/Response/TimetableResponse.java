package fr.epita.dto.Response;

import lombok.Builder;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

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

    /** Names of programmes running in this cohort — a slot can serve multiple programmes. */
    private List<String> programmeNames;

    private Long lecturerId;
    private String lecturerName;

    private Long universityId;
    private String universityName;
}
