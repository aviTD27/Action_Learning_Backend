package fr.epita.service;

import fr.epita.dto.Request.GradeRequest;
import fr.epita.dto.Response.GradeResponse;
import fr.epita.enums.GradeStatus;
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
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class GradeService {

    private final StudentGradeRepository studentGradeRepository;
    private final SubmissionRepository submissionRepository;
    private final StudentRepository studentRepository;

    public List<GradeResponse> getForSubmission(Long submissionId) {
        findSubmission(submissionId); // 404 when the submission doesn't exist
        return studentGradeRepository.findBySubmissionId(submissionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public GradeResponse setGrade(Long submissionId, Long studentId, GradeRequest request) {
        Submission submission = findSubmission(submissionId);
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        if (!Objects.equals(student.getCohort() != null ? student.getCohort().getId() : null,
                submission.getCohort().getId())) {
            throw new IllegalStateException("Student is not in this submission's cohort");
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

        grade.setGrade(request.getGrade());
        grade.setFeedback(request.getFeedback());
        grade.setStatus(GradeStatus.DRAFT);
        grade.setGradedAt(Instant.now());

        return toResponse(studentGradeRepository.save(grade));
    }

    @Transactional
    public List<GradeResponse> releaseAll(Long submissionId) {
        findSubmission(submissionId);
        List<StudentGrade> grades = studentGradeRepository.findBySubmissionId(submissionId);
        for (StudentGrade grade : grades) {
            grade.setStatus(GradeStatus.RELEASED);
        }
        return studentGradeRepository.saveAll(grades)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private Submission findSubmission(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found"));
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
