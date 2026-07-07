package fr.epita.service;

import fr.epita.dto.Request.CreateSubmissionRequest;
import fr.epita.dto.Request.SubmissionRulesRequest;
import fr.epita.dto.Response.SubmissionResponse;
import fr.epita.enums.CourseStatus;
import fr.epita.enums.NotificationType;
import fr.epita.enums.SubmissionStatus;
import fr.epita.enums.SubmissionType;
import fr.epita.model.Course;
import fr.epita.model.Lecturer;
import fr.epita.model.Submission;
import fr.epita.model.SubmissionRules;
import fr.epita.repository.CourseRepository;
import fr.epita.repository.LecturerRepository;
import fr.epita.repository.NotificationRepository;
import fr.epita.repository.StudentGradeRepository;
import fr.epita.repository.SubmissionRepository;
import fr.epita.repository.SubmissionUploadRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final StudentGradeRepository studentGradeRepository;
    private final SubmissionUploadRepository uploadRepository;
    private final CourseRepository courseRepository;
    private final LecturerRepository lecturerRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /**
     * @param courseId     when set, assignments of a single course
     * @param lecturerId   when set, assignments a lecturer owns
     * @param universityId when set, all assignments in a university (course -> programme -> university)
     * @param programmeId  when set, all assignments of a programme (student view)
     * @param studentView  when true, only PUBLISHED assignments are returned (drafts/archived hidden).
     */
    public List<SubmissionResponse> getAll(Long courseId, Long lecturerId, Long universityId,
                                           Long programmeId, boolean studentView) {
        List<Submission> submissions;
        if (courseId != null) {
            submissions = submissionRepository.findByCourseId(courseId);
        } else if (lecturerId != null) {
            submissions = submissionRepository.findByLecturerId(lecturerId);
        } else if (programmeId != null) {
            submissions = submissionRepository.findByCourse_Programme_Id(programmeId);
        } else if (universityId != null) {
            submissions = submissionRepository.findByCourse_Programme_University_Id(universityId);
        } else {
            submissions = submissionRepository.findAll();
        }
        return submissions.stream()
                .filter(s -> !studentView || s.getStatus() == null || s.getStatus() == SubmissionStatus.PUBLISHED)
                .map(this::toResponse)
                .toList();
    }

    public SubmissionResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public SubmissionResponse create(CreateSubmissionRequest request) {
        // Support "one or more courses": create one assignment per selected course.
        List<Long> courseIds = (request.getCourseIds() != null && !request.getCourseIds().isEmpty())
                ? request.getCourseIds()
                : List.of(request.getCourseId());

        if (courseIds.isEmpty() || courseIds.get(0) == null) {
            throw new IllegalStateException("At least one course is required");
        }

        SubmissionStatus status = request.getStatus() != null ? request.getStatus() : SubmissionStatus.DRAFT;
        SubmissionType type = request.getSubmissionType() != null ? request.getSubmissionType() : SubmissionType.BOTH;

        List<Submission> created = new ArrayList<>();
        for (Long cid : courseIds) {
            Course course = courseRepository.findById(cid)
                    .orElseThrow(() -> new EntityNotFoundException("Course not found"));
            if (course.getStatus() == CourseStatus.ARCHIVED) {
                throw new IllegalStateException("Course \"" + course.getName() + "\" is archived");
            }

            // Default the assignment's lecturer to the course's teaching lecturer when not supplied.
            Long lecturerId = request.getLecturerId() != null
                    ? request.getLecturerId()
                    : (course.getLecturer() != null ? course.getLecturer().getId() : null);

            Submission submission = Submission.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .instructions(request.getInstructions())
                    .additionalNotes(request.getAdditionalNotes())
                    .submissionType(type)
                    .status(status)
                    .course(course)
                    .lecturer(resolveLecturer(lecturerId))
                    .dueDate(request.getDueDate())
                    .dueTime(request.getDueTime())
                    .maxPoints(request.getMaxPoints())
                    .rules(toRules(request))
                    .templateFileName(request.getTemplateFileName())
                    .build();

            Submission saved = submissionRepository.save(submission);

            if (status == SubmissionStatus.PUBLISHED) {
                notificationService.notifyCourseStudents(saved, NotificationType.NEW_SUBMISSION,
                        "New assignment: \"" + saved.getTitle() + "\" — due " + saved.deadline() + ".");
            }
            created.add(saved);
        }
        return toResponse(created.get(0));
    }

    @Transactional
    public SubmissionResponse update(Long id, CreateSubmissionRequest request) {
        Submission submission = find(id);

        if (submission.getStatus() == SubmissionStatus.PUBLISHED
                && LocalDateTime.now().isAfter(submission.deadline())) {
            LocalDateTime newDeadline = request.getDueDate() != null
                    ? request.getDueDate().atTime(request.getDueTime() != null ? request.getDueTime() : LocalTime.of(23, 59))
                    : submission.deadline();
            if (!newDeadline.isAfter(LocalDateTime.now())) {
                throw new IllegalStateException(
                        "This assignment's deadline has passed. Set a new due date in the future to re-open it for editing.");
            }
        }

        Course course = courseRepository.findById(
                        request.getCourseId() != null ? request.getCourseId() : submission.getCourse().getId())
                .orElseThrow(() -> new EntityNotFoundException("Course not found"));

        submission.setTitle(request.getTitle());
        submission.setDescription(request.getDescription());
        submission.setInstructions(request.getInstructions());
        submission.setAdditionalNotes(request.getAdditionalNotes());
        if (request.getSubmissionType() != null) submission.setSubmissionType(request.getSubmissionType());
        submission.setCourse(course);
        submission.setDueDate(request.getDueDate());
        submission.setDueTime(request.getDueTime());
        submission.setMaxPoints(request.getMaxPoints());
        submission.setRules(toRules(request));
        submission.setTemplateFileName(request.getTemplateFileName());
        if (request.getLecturerId() != null) {
            submission.setLecturer(resolveLecturer(request.getLecturerId()));
        }

        Submission saved = submissionRepository.save(submission);

        if (uploadRepository.countBySubmissionId(saved.getId()) > 0) {
            notificationService.notifyCourseStudents(saved, NotificationType.ASSIGNMENT_EDITED,
                    "Assignment updated: \"" + saved.getTitle() + "\". The instructions changed — please review.");
            saved.setLastNotifiedAt(Instant.now());
            saved = submissionRepository.save(saved);
        }
        return toResponse(saved);
    }

    @Transactional
    public SubmissionResponse publish(Long id) {
        Submission submission = find(id);
        submission.setStatus(SubmissionStatus.PUBLISHED);
        Submission saved = submissionRepository.save(submission);
        notificationService.notifyCourseStudents(saved, NotificationType.NEW_SUBMISSION,
                "New assignment: \"" + saved.getTitle() + "\" — due " + saved.deadline() + ".");
        return toResponse(saved);
    }

    @Transactional
    public SubmissionResponse archive(Long id) {
        Submission submission = find(id);
        submission.setStatus(SubmissionStatus.ARCHIVED);
        return toResponse(submissionRepository.save(submission));
    }

    @Transactional
    public SubmissionResponse unarchive(Long id) {
        Submission submission = find(id);
        if (submission.getStatus() != SubmissionStatus.ARCHIVED) {
            throw new IllegalStateException("Only an archived assignment can be unarchived.");
        }
        submission.setStatus(SubmissionStatus.PUBLISHED);
        return toResponse(submissionRepository.save(submission));
    }

    @Transactional
    public void delete(Long id) {
        Submission submission = find(id);
        boolean hasSubmissions = uploadRepository.countBySubmissionId(submission.getId()) > 0;
        if (submission.getStatus() != SubmissionStatus.DRAFT && hasSubmissions) {
            throw new IllegalStateException(
                    "This assignment has student submissions and can only be archived, not deleted.");
        }
        // Delete related data before deleting the submission
        notificationRepository.deleteBySubmissionId(submission.getId());
        studentGradeRepository.deleteBySubmissionId(submission.getId());
        submissionRepository.delete(submission);
    }

    @Transactional
    public SubmissionResponse reopenForStudent(Long id, Long studentId) {
        Submission submission = find(id);
        submission.getReopenedStudentIds().add(studentId);
        return toResponse(submissionRepository.save(submission));
    }

    @Transactional
    public SubmissionResponse notifyStudents(Long id) {
        Submission submission = find(id);
        notificationService.notifyNonSubmitters(submission, NotificationType.MANUAL,
                "Reminder from your lecturer: you have not submitted \"" + submission.getTitle()
                        + "\" yet — it is due " + submission.deadline() + ".");
        submission.setLastNotifiedAt(Instant.now());
        return toResponse(submissionRepository.save(submission));
    }

    @Transactional
    public SubmissionResponse saveTemplate(Long id, MultipartFile file) throws IOException {
        Submission submission = find(id);
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "template";
        Path dir = Paths.get(uploadDir, "templates", String.valueOf(id));
        Files.createDirectories(dir);
        Path target = dir.resolve(originalName);
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        submission.setTemplateFileName(originalName);
        submission.setTemplateStoredPath(target.toAbsolutePath().toString());
        return toResponse(submissionRepository.save(submission));
    }

    public ResponseEntity<Resource> downloadTemplate(Long id) {
        Submission submission = find(id);
        if (submission.getTemplateStoredPath() == null) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(Path.of(submission.getTemplateStoredPath()));
        if (!resource.exists()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + submission.getTemplateFileName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }

    private Submission find(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found"));
    }

    /**
     * Get the Lecturer ID for a given email address.
     * Used to ensure lecturers can only see their own submissions.
     */
    public Long getLecturerIdByEmail(String email) {
        return lecturerRepository.findByEmail(email)
                .map(Lecturer::getId)
                .orElse(null);
    }

    private Lecturer resolveLecturer(Long lecturerId) {
        if (lecturerId == null) return null;
        return lecturerRepository.findById(lecturerId)
                .orElseThrow(() -> new EntityNotFoundException("Lecturer not found"));
    }

    private SubmissionRules toRules(CreateSubmissionRequest request) {
        SubmissionRulesRequest r = request.getRules();
        return SubmissionRules.builder()
                .allowedFileTypes(r.getAllowedFileTypes())
                .maxAttempts(r.getMaxAttempts())
                .lateAllowed(r.isLateAllowed())
                .minWordCount(r.getMinWordCount())
                .maxWordCount(r.getMaxWordCount())
                .maxFileSizeBytes(r.getMaxFileSizeBytes())
                .namingPattern(r.getNamingPattern())
                .requiredHeadings(r.getRequiredHeadings())
                .build();
    }

    private SubmissionResponse toResponse(Submission s) {
        SubmissionRules r = s.getRules();
        Course course = s.getCourse();
        return SubmissionResponse.builder()
                .id(s.getId())
                .title(s.getTitle())
                .description(s.getDescription())
                .instructions(s.getInstructions())
                .additionalNotes(s.getAdditionalNotes())
                .submissionType(s.getSubmissionType() != null ? s.getSubmissionType().name() : SubmissionType.BOTH.name())
                .status(s.getStatus() != null ? s.getStatus().name() : SubmissionStatus.PUBLISHED.name())
                .courseId(course != null ? course.getId() : null)
                .courseName(course != null ? course.getName() : null)
                .programmeId(course != null && course.getProgramme() != null ? course.getProgramme().getId() : null)
                .programmeName(course != null && course.getProgramme() != null ? course.getProgramme().getName() : null)
                .lecturerId(s.getLecturer() != null ? s.getLecturer().getId() : null)
                .dueDate(s.getDueDate())
                .dueTime(s.getDueTime())
                .maxPoints(s.getMaxPoints())
                .allowedFileTypes(r != null ? r.getAllowedFileTypes() : null)
                .maxAttempts(r != null ? r.getMaxAttempts() : 1)
                .lateAllowed(r != null && r.isLateAllowed())
                .minWordCount(r != null ? r.getMinWordCount() : null)
                .maxWordCount(r != null ? r.getMaxWordCount() : null)
                .maxFileSizeBytes(r != null ? r.getMaxFileSizeBytes() : null)
                .namingPattern(r != null ? r.getNamingPattern() : null)
                .requiredHeadings(r != null ? r.getRequiredHeadings() : null)
                .templateFileName(s.getTemplateFileName())
                .hasTemplate(s.getTemplateStoredPath() != null)
                .hasTemplateFile(s.getTemplateStoredPath() != null && !s.getTemplateStoredPath().isBlank())
                .lastNotifiedAt(s.getLastNotifiedAt())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
