package fr.epita.service;

import fr.epita.dto.Request.CreateCourseRequest;
import fr.epita.dto.Response.CourseResponse;
import fr.epita.enums.CourseStatus;
import fr.epita.model.Course;
import fr.epita.model.Lecturer;
import fr.epita.model.Semester;
import fr.epita.repository.CourseRepository;
import fr.epita.repository.LecturerRepository;
import fr.epita.repository.SemesterRepository;
import fr.epita.repository.StudentRepository;
import fr.epita.repository.SubmissionRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;
    private final SemesterRepository semesterRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;
    private final SubmissionRepository submissionRepository;

    /**
     * Lists courses, scoped by (in priority order) semester, programme, lecturer or university.
     */
    public List<CourseResponse> getAll(Long semesterId, Long programmeId, Long lecturerId, Long universityId) {
        List<Course> courses;
        if (semesterId != null) {
            courses = courseRepository.findBySemesterId(semesterId);
        } else if (programmeId != null) {
            courses = courseRepository.findByProgrammeId(programmeId);
        } else if (lecturerId != null) {
            courses = courseRepository.findByLecturerId(lecturerId);
        } else if (universityId != null) {
            courses = courseRepository.findByProgramme_UniversityId(universityId);
        } else {
            courses = courseRepository.findAll();
        }
        return courses.stream().map(this::toResponse).toList();
    }

    public CourseResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public CourseResponse create(CreateCourseRequest request) {
        Semester semester = semesterRepository.findById(request.getSemesterId())
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));

        Course course = Course.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .semester(semester)
                .programme(semester.getProgramme())   // denormalised from the semester
                .lecturer(resolveLecturer(request.getLecturerId()))
                .status(CourseStatus.ACTIVE)
                .build();
        return toResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse update(Long id, CreateCourseRequest request) {
        Course course = find(id);
        course.setName(request.getName());
        course.setCode(request.getCode());
        course.setDescription(request.getDescription());
        if (request.getSemesterId() != null) {
            Semester semester = semesterRepository.findById(request.getSemesterId())
                    .orElseThrow(() -> new EntityNotFoundException("Semester not found"));
            course.setSemester(semester);
            course.setProgramme(semester.getProgramme());
        }
        course.setLecturer(resolveLecturer(request.getLecturerId()));
        return toResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse archive(Long id) {
        Course course = find(id);
        course.setStatus(CourseStatus.ARCHIVED);
        return toResponse(courseRepository.save(course));
    }

    @Transactional
    public CourseResponse unarchive(Long id) {
        Course course = find(id);
        course.setStatus(CourseStatus.ACTIVE);
        return toResponse(courseRepository.save(course));
    }

    @Transactional
    public void delete(Long id) {
        Course course = find(id);
        if (!submissionRepository.findByCourseId(id).isEmpty()) {
            throw new IllegalStateException("Cannot delete a course that has assignments. Archive it instead.");
        }
        courseRepository.delete(course);
    }

    private Course find(Long id) {
        return courseRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));
    }

    private Lecturer resolveLecturer(Long lecturerId) {
        if (lecturerId == null) return null;
        return lecturerRepository.findById(lecturerId)
                .orElseThrow(() -> new EntityNotFoundException("Lecturer not found"));
    }

    private CourseResponse toResponse(Course c) {
        int studentCount = c.getProgramme() != null
                ? studentRepository.findByProgrammeId(c.getProgramme().getId()).size() : 0;
        int assignmentCount = submissionRepository.findByCourseId(c.getId()).size();
        return CourseResponse.builder()
                .id(c.getId())
                .name(c.getName())
                .code(c.getCode())
                .description(c.getDescription())
                .status(c.getStatus() != null ? c.getStatus().name() : CourseStatus.ACTIVE.name())
                .semesterId(c.getSemester() != null ? c.getSemester().getId() : null)
                .semesterName(c.getSemester() != null ? c.getSemester().getName() : null)
                .programmeId(c.getProgramme() != null ? c.getProgramme().getId() : null)
                .programmeName(c.getProgramme() != null ? c.getProgramme().getName() : null)
                .lecturerId(c.getLecturer() != null ? c.getLecturer().getId() : null)
                .lecturerName(c.getLecturer() != null
                        ? c.getLecturer().getFirstName() + " " + c.getLecturer().getLastName() : null)
                .studentCount(studentCount)
                .assignmentCount(assignmentCount)
                .build();
    }
}
