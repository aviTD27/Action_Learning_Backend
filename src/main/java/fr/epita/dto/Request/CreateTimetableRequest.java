package fr.epita.dto.Request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Data
public class CreateTimetableRequest {

    @NotBlank
    private String title;

    private String room;

    @NotNull
    private DayOfWeek dayOfWeek;

    @NotNull
    private LocalTime startTime;

    @NotNull
    private LocalTime endTime;

    private String color;

    @NotNull
    private Long cohortId;

    private Long lecturerId;
}
