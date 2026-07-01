package fr.epita.service;

import fr.epita.dto.Request.CreateSubmissionRequest;
import fr.epita.dto.Request.SubmissionRulesRequest;
import fr.epita.dto.Response.SubmissionResponse;
import fr.epita.enums.CohortStatus;
import fr.epita.enums.NotificationType;
import fr.epita.model.Cohort;
import fr.epita.model.Lecturer;
import fr.epita.model.Submission;
import fr.epita.model.SubmissionRules;
import fr.epita.repository.CohortRepository;
import fr.epita.repository.LecturerRepository;
import fr.epita.repository.StudentGradeRepository;
import fr.epita.repository.SubmissionRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final StudentGradeRepository studentGradeRepository;
    private final CohortRepository cohortRepository;
    private final LecturerRepository lecturerRepository;
    private final NotificationService notificationService;

    public List<SubmissionResponse> getAll(Long cohortId, Long lecturerId, Long universityId) {
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
        return submissions.stream().map(this::toResponse).toList();
    }

    public SubmissionResponse getById(Long id) {
        return toResponse(find(id));
    }

    @Transactional
    public SubmissionResponse create(CreateSubmissionRequest request) {
        Cohort cohort = cohortRepository.findById(request.getCohortId())
                .orElseThrow(() -> new EntityNotFoundException("Cohort not found"));

        if (cohort.getStatus() != CohortStatus.ONGOING) {
            throw new IllegalStateException("Cohort is not ongoing");
        }

        Submission submission = Submission.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .cohort(cohort)
                .lecturer(resolveLecturer(request.getLecturerId()))
                .dueDate(request.getDueDate())
                .maxPoints(request.getMaxPoints())
                .rules(toRules(request))
                .templateFileName(request.getTemplateFileName())
                .build();

        Submission saved = submissionRepository.save(submission);

        notificationService.notifyCohort(saved, NotificationType.NEW_SUBMISSION,
                "New submission: \"" + saved.getTitle() + "\" — due " + saved.getDueDate() + " 23:59.");

        return toResponse(saved);
    }

    @Transactional
    public SubmissionResponse update(Long id, CreateSubmissionRequest request) {
        Submission submission = find(id);
        Cohort cohort = cohortRepository.findById(request.getCohortId())
                .orElseThrow(() -> new EntityNotFoundException("Cohort not found"));

        submission.setTitle(request.getTitle());
        submission.setDescription(request.getDescription());
        submission.setCohort(cohort);
        submission.setDueDate(request.getDueDate());
        submission.setMaxPoints(request.getMaxPoints());
        submission.setRules(toRules(request));
        submission.setTemplateFileName(request.getTemplateFileName());
        if (request.getLecturerId() != null) {
            submission.setLecturer(resolveLecturer(request.getLecturerId()));
        }

        return toResponse(submissionRepository.save(submission));
    }

    @Transactional
    public void delete(Long id) {
        Submission submission = find(id);
        studentGradeRepository.deleteBySubmissionId(submission.getId());
        submissionRepository.delete(submission);
    }

    //TODO Email Option. 
    @Transactional
    public SubmissionResponse notifyStudents(Long id) {
        Submission submission = find(id);
        notificationService.notifyCohort(submission, NotificationType.MANUAL,
                "Reminder from your lecturer: \"" + submission.getTitle()
                        + "\" is due " + submission.getDueDate() + " 23:59.");
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
                .cohortId(s.getCohort().getId())
                .cohortName(s.getCohort().getName())
                .lecturerId(s.getLecturer() != null ? s.getLecturer().getId() : null)
                .dueDate(s.getDueDate())
                .maxPoints(s.getMaxPoints())
                .allowedFileTypes(r != null ? r.getAllowedFileTypes() : null)
                .maxAttempts(r != null ? r.getMaxAttempts() : 1)
                .lateAllowed(r != null && r.isLateAllowed())
                .minWordCount(r != null ? r.getMinWordCount() : null)
                .maxWordCount(r != null ? r.getMaxWordCount() : null)
                .namingPattern(r != null ? r.getNamingPattern() : null)
                .requiredHeadings(r != null ? r.getRequiredHeadings() : null)
                .templateFileName(s.getTemplateFileName())
                .lastNotifiedAt(s.getLastNotifiedAt())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
