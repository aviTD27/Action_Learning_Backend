package fr.epita.service;

import fr.epita.dto.Request.CreateSubmissionRequest;
import fr.epita.dto.Request.SubmissionRulesRequest;
import fr.epita.dto.Response.SubmissionResponse;
import fr.epita.enums.CohortStatus;
import fr.epita.enums.NotificationType;
import fr.epita.enums.SubmissionStatus;
import fr.epita.enums.SubmissionType;
import fr.epita.model.Cohort;
import fr.epita.model.Lecturer;
import fr.epita.model.Submission;
import fr.epita.model.SubmissionRules;
import fr.epita.repository.CohortRepository;
import fr.epita.repository.LecturerRepository;
import fr.epita.repository.StudentGradeRepository;
import fr.epita.repository.SubmissionRepository;
import fr.epita.repository.SubmissionUploadRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final StudentGradeRepository studentGradeRepository;
    private final SubmissionUploadRepository uploadRepository;
    private final CohortRepository cohortRepository;
    private final LecturerRepository lecturerRepository;
    private final NotificationService notificationService;

    /**
     * @param studentView when true, only PUBLISHED assignments are returned (drafts/archived are hidden).
     */
    public List<SubmissionResponse> getAll(Long cohortId, Long lecturerId, Long universityId, boolean studentView) {
        List<Submission> submissions;
        if (cohortId != null) {
            submissions = submissionRepository.findByCohortId(cohortId);
        } else if (lecturerId != null) {
            submissions = submissionRepository.findByLecturerId(lecturerId);
        } else if (universityId != null) {
            submissions = submissionRepository.findByCohort_Programme_University_Id(universityId);
        } else {
            submissions = submissionRepository.findAll();
        }
        return submissions.stream()
                .filter(s -> !studentView || s.getStatus() == SubmissionStatus.PUBLISHED)
                .map(this::toResponse)
                .toList();
    }

    public SubmissionResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public SubmissionResponse create(CreateSubmissionRequest request) {
        // Support "one or more cohorts": create one assignment per selected cohort.
        List<Long> cohortIds = (request.getCohortIds() != null && !request.getCohortIds().isEmpty())
                ? request.getCohortIds()
                : List.of(request.getCohortId());

        if (cohortIds.isEmpty() || cohortIds.get(0) == null) {
            throw new IllegalStateException("At least one cohort is required");
        }

        SubmissionStatus status = request.getStatus() != null ? request.getStatus() : SubmissionStatus.DRAFT;
        SubmissionType type = request.getSubmissionType() != null ? request.getSubmissionType() : SubmissionType.BOTH;

        List<Submission> created = new ArrayList<>();
        for (Long cid : cohortIds) {
            Cohort cohort = cohortRepository.findById(cid)
                    .orElseThrow(() -> new EntityNotFoundException("Cohort not found"));
            if (cohort.getStatus() != CohortStatus.ONGOING) {
                throw new IllegalStateException("Cohort \"" + cohort.getName() + "\" is not ongoing");
            }

            Submission submission = Submission.builder()
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .additionalNotes(request.getAdditionalNotes())
                    .submissionType(type)
                    .status(status)
                    .cohort(cohort)
                    .lecturer(resolveLecturer(request.getLecturerId()))
                    .dueDate(request.getDueDate())
                    .dueTime(request.getDueTime())
                    .maxPoints(request.getMaxPoints())
                    .rules(toRules(request))
                    .templateFileName(request.getTemplateFileName())
                    .build();

            Submission saved = submissionRepository.save(submission);

            // Only a published assignment is visible to — and notified to — students.
            if (status == SubmissionStatus.PUBLISHED) {
                notificationService.notifyCohort(saved, NotificationType.NEW_SUBMISSION,
                        "New assignment: \"" + saved.getTitle() + "\" — due " + saved.deadline() + ".");
            }
            created.add(saved);
        }
        return toResponse(created.get(0));
    }

    @Transactional
    public SubmissionResponse update(Long id, CreateSubmissionRequest request) {
        Submission submission = find(id);

        // Editable until the deadline (row 72): a published assignment past its deadline is locked.
        if (submission.getStatus() == SubmissionStatus.PUBLISHED
                && LocalDateTime.now().isAfter(submission.deadline())) {
            throw new IllegalStateException("This assignment's deadline has passed and it can no longer be edited.");
        }

        Cohort cohort = cohortRepository.findById(
                        request.getCohortId() != null ? request.getCohortId() : submission.getCohort().getId())
                .orElseThrow(() -> new EntityNotFoundException("Cohort not found"));

        submission.setTitle(request.getTitle());
        submission.setDescription(request.getDescription());
        submission.setAdditionalNotes(request.getAdditionalNotes());
        if (request.getSubmissionType() != null) submission.setSubmissionType(request.getSubmissionType());
        submission.setCohort(cohort);
        submission.setDueDate(request.getDueDate());
        submission.setDueTime(request.getDueTime());
        submission.setMaxPoints(request.getMaxPoints());
        submission.setRules(toRules(request));
        submission.setTemplateFileName(request.getTemplateFileName());
        if (request.getLecturerId() != null) {
            submission.setLecturer(resolveLecturer(request.getLecturerId()));
        }

        Submission saved = submissionRepository.save(submission);

        // If students have already submitted, an edit notifies the affected students (row 72).
        if (uploadRepository.countBySubmissionId(saved.getId()) > 0) {
            notificationService.notifyCohort(saved, NotificationType.MANUAL,
                    "Assignment updated: \"" + saved.getTitle() + "\". Please review the changes.");
            saved.setLastNotifiedAt(Instant.now());
            saved = submissionRepository.save(saved);
        }
        return toResponse(saved);
    }

    /** Publishes a draft assignment — becomes visible to the cohort and notifies students (row 71). */
    @Transactional
    public SubmissionResponse publish(Long id) {
        Submission submission = find(id);
        submission.setStatus(SubmissionStatus.PUBLISHED);
        Submission saved = submissionRepository.save(submission);
        notificationService.notifyCohort(saved, NotificationType.NEW_SUBMISSION,
                "New assignment: \"" + saved.getTitle() + "\" — due " + saved.deadline() + ".");
        return toResponse(saved);
    }

    /** Archives an assignment — hidden from students but kept for reference (row 73). */
    @Transactional
    public SubmissionResponse archive(Long id) {
        Submission submission = find(id);
        submission.setStatus(SubmissionStatus.ARCHIVED);
        return toResponse(submissionRepository.save(submission));
    }

    /** Restores an archived assignment back to published (visible again, no re-notification). */
    @Transactional
    public SubmissionResponse unarchive(Long id) {
        Submission submission = find(id);
        if (submission.getStatus() != SubmissionStatus.ARCHIVED) {
            throw new IllegalStateException("Only an archived assignment can be unarchived.");
        }
        submission.setStatus(SubmissionStatus.PUBLISHED);
        return toResponse(submissionRepository.save(submission));
    }

    /**
     * Delete vs archive (row 73): unpublished (draft) assignments can be deleted;
     * a published assignment that already has submissions can only be archived.
     */
    @Transactional
    public void delete(Long id) {
        Submission submission = find(id);
        boolean hasSubmissions = uploadRepository.countBySubmissionId(submission.getId()) > 0;
        if (submission.getStatus() != SubmissionStatus.DRAFT && hasSubmissions) {
            throw new IllegalStateException(
                    "This assignment has student submissions and can only be archived, not deleted.");
        }
        studentGradeRepository.deleteBySubmissionId(submission.getId());
        submissionRepository.delete(submission);
    }

    /** Re-opens a closed assignment for one student as a late exception (row 77). */
    @Transactional
    public SubmissionResponse reopenForStudent(Long id, Long studentId) {
        Submission submission = find(id);
        submission.getReopenedStudentIds().add(studentId);
        return toResponse(submissionRepository.save(submission));
    }

    @Transactional
    public SubmissionResponse notifyStudents(Long id) {
        Submission submission = find(id);
        notificationService.notifyCohort(submission, NotificationType.MANUAL,
                "Reminder from your lecturer: \"" + submission.getTitle()
                        + "\" is due " + submission.deadline() + ".");
        submission.setLastNotifiedAt(Instant.now());
        return toResponse(submissionRepository.save(submission));
    }

    private Submission find(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found"));
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
        return SubmissionResponse.builder()
                .id(s.getId())
                .title(s.getTitle())
                .description(s.getDescription())
                .additionalNotes(s.getAdditionalNotes())
                .submissionType(s.getSubmissionType() != null ? s.getSubmissionType().name() : SubmissionType.BOTH.name())
                .status(s.getStatus() != null ? s.getStatus().name() : SubmissionStatus.PUBLISHED.name())
                .cohortId(s.getCohort().getId())
                .cohortName(s.getCohort().getName())
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
                .hasTemplateFile(s.getTemplateStoredPath() != null && !s.getTemplateStoredPath().isBlank())
                .lastNotifiedAt(s.getLastNotifiedAt())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
