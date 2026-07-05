package fr.epita.service;

import fr.epita.dto.Request.GradeRequest;
import fr.epita.dto.Response.GradeResponse;
import fr.epita.dto.Response.MyGradeResponse;
import fr.epita.enums.GradeStatus;
import fr.epita.enums.NotificationType;
import fr.epita.model.Student;
import fr.epita.model.StudentGrade;
import fr.epita.model.Submission;
import fr.epita.repository.StudentGradeRepository;
import fr.epita.repository.StudentRepository;
import fr.epita.repository.SubmissionRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final StudentGradeRepository studentGradeRepository;
    private final SubmissionRepository submissionRepository;
    private final StudentRepository studentRepository;
    private final NotificationService notificationService;

    public List<GradeResponse> getForSubmission(Long submissionId) {
        findSubmission(submissionId); // 404 when the submission doesn't exist
        return studentGradeRepository.findBySubmissionId(submissionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MyGradeResponse> getMyGrades(String email) {
        return studentGradeRepository.findByStudentEmailAndStatus(email, GradeStatus.RELEASED)
                .stream()
                .map(this::toMyGradeResponse)
                .toList();
    }

    @Transactional
    public GradeResponse setGrade(Long submissionId, Long studentId, GradeRequest request) {
        Submission submission = findSubmission(submissionId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        Long studentProgrammeId = student.getProgramme() != null ? student.getProgramme().getId() : null;
        Long submissionProgrammeId = submission.getCourse() != null && submission.getCourse().getProgramme() != null
                ? submission.getCourse().getProgramme().getId() : null;
        if (!Objects.equals(studentProgrammeId, submissionProgrammeId)) {
            throw new IllegalStateException("Student is not enrolled in this assignment's programme");
        }
        if (request.getGrade() < 0 || request.getGrade() > submission.getMaxPoints()) {
            throw new IllegalStateException(
                    "Grade must be between 0 and " + submission.getMaxPoints());
        }

        StudentGrade grade = studentGradeRepository
                .findBySubmissionIdAndStudentId(submissionId, studentId)
                .orElseGet(() -> StudentGrade.builder()
                        .submission(submission)
                        .student(student)
                        .build());

        // Row 111 — editing an already-RELEASED grade keeps it released and notifies the student.
        boolean wasReleased = grade.getId() != null && grade.getStatus() == GradeStatus.RELEASED;

        grade.setGrade(request.getGrade());
        grade.setFeedback(request.getFeedback());
        grade.setStatus(wasReleased ? GradeStatus.RELEASED : GradeStatus.DRAFT);
        grade.setGradedAt(Instant.now());
        StudentGrade saved = studentGradeRepository.save(grade);

        if (wasReleased) {
            notificationService.notifyStudent(student, submission, NotificationType.GRADE_UPDATED,
                    "Your grade for \"" + submission.getTitle() + "\" has been updated to "
                            + fmt(saved.getGrade()) + " / " + submission.getMaxPoints() + ".");
        }
        return toResponse(saved);
    }

    @Transactional
    public List<GradeResponse> releaseAll(Long submissionId) {
        Submission submission = findSubmission(submissionId);
        List<StudentGrade> grades = studentGradeRepository.findBySubmissionId(submissionId);

        // Row 110 — only newly released grades trigger a "grade released" notification.
        Instant now = Instant.now();
        List<StudentGrade> newlyReleased = new ArrayList<>();
        for (StudentGrade grade : grades) {
            if (grade.getStatus() != GradeStatus.RELEASED) {
                grade.setStatus(GradeStatus.RELEASED);
                grade.setReleasedAt(now);
                newlyReleased.add(grade);
            }
        }
        List<GradeResponse> result = studentGradeRepository.saveAll(grades)
                .stream()
                .map(this::toResponse)
                .toList();

        for (StudentGrade grade : newlyReleased) {
            notificationService.notifyStudent(grade.getStudent(), submission, NotificationType.GRADE_RELEASED,
                    "Your grade for \"" + submission.getTitle() + "\" has been released: "
                            + fmt(grade.getGrade()) + " / " + submission.getMaxPoints() + ".");
        }
        return result;
    }

    private String fmt(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }

    private Submission findSubmission(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found"));
    }

    private MyGradeResponse toMyGradeResponse(StudentGrade g) {
        boolean revised = g.getReleasedAt() != null
                && g.getGradedAt().isAfter(g.getReleasedAt());
        return MyGradeResponse.builder()
                .submissionId(g.getSubmission().getId())
                .submissionTitle(g.getSubmission().getTitle())
                .maxPoints(g.getSubmission().getMaxPoints())
                .grade(g.getGrade())
                .feedback(g.getFeedback())
                .gradedAt(g.getGradedAt())
                .releasedAt(g.getReleasedAt())
                .revised(revised)
                .build();
    }

    private GradeResponse toResponse(StudentGrade g) {
        return GradeResponse.builder()
                .studentId(g.getStudent().getId())
                .studentName(g.getStudent().getFirstName() + " " + g.getStudent().getLastName())
                .studentRef(g.getStudent().getStudentRef())
                .grade(g.getGrade())
                .feedback(g.getFeedback())
                .status(g.getStatus().name())
                .gradedAt(g.getGradedAt())
                .build();
    }
}
