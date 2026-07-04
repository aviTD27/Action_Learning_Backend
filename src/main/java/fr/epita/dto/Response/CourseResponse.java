package fr.epita.dto.Response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseResponse {
    private Long id;
    private String name;
    private String code;
    private String description;
    private String status;
    private Long semesterId;
    private String semesterName;
    private Long programmeId;
    private String programmeName;
    private Long lecturerId;
    private String lecturerName;
    private int studentCount;
    private int assignmentCount;
}
