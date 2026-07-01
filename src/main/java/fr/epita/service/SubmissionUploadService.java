package fr.epita.service;

import fr.epita.dto.Response.ComplianceReportResponse;
import fr.epita.dto.Response.MyUploadStatusResponse;
import fr.epita.dto.Response.StudentSubmissionResponse;
import fr.epita.model.Student;
import fr.epita.model.Submission;
import fr.epita.model.SubmissionUpload;
import fr.epita.repository.StudentRepository;
import fr.epita.repository.SubmissionUploadRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubmissionUploadService {

    private final AiServiceClient aiServiceClient;
    private final SubmissionUploadRepository uploadRepository;
    private final StudentRepository studentRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Transactional
    public ComplianceReportResponse processUpload(
            Submission submission, Student student, MultipartFile file) throws IOException {

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";

        ComplianceReportResponse report = aiServiceClient.check(file, submission.getRules());

        String storedPath = "";
        String detectedType = "REJECTED";
        boolean fileTypeOk = report.getFileType() != null && report.getFileType().isPassed();
        if (fileTypeOk) {
            String ext = getExtension(originalName);
            detectedType = ext.toUpperCase();
            storedPath = saveFile(file, submission.getId(), student.getId(), ext);
        }

        SubmissionUpload upload = SubmissionUpload.builder()
                .submission(submission)
                .student(student)
                .originalFileName(originalName)
                .storedPath(storedPath)
                .detectedFileType(detectedType)
                .fileSizeBytes(file.getSize())
                .compliancePassed(report.isOverallPass())
                .complianceReportJson("")
                .uploadedAt(Instant.now())
                .build();

        SubmissionUpload saved = uploadRepository.save(upload);
        report.setUploadId(saved.getId());
        return report;
    }

    @Transactional
    public void turnIn(Long uploadId, String studentEmail) {
        SubmissionUpload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new EntityNotFoundException("Upload not found"));
        if (!upload.getStudent().getEmail().equals(studentEmail))
            throw new SecurityException("Not your upload");
        if (!upload.isCompliancePassed())
            throw new IllegalStateException("Cannot turn in a document that failed compliance checks.");
        if (Boolean.TRUE.equals(upload.getTurnedIn()))
            throw new IllegalStateException("Document already turned in.");
        upload.setTurnedIn(true);
        upload.setTurnedInAt(Instant.now());
        uploadRepository.save(upload);
    }

    public List<StudentSubmissionResponse> getStudentSubmissions(Submission submission) {
        return uploadRepository.findBySubmissionIdAndTurnedInTrue(submission.getId())
                .stream()
                .map(u -> {
                    Student s = u.getStudent();
                    long attempts = uploadRepository.countBySubmissionIdAndStudentId(submission.getId(), s.getId());
                    boolean late = u.getTurnedInAt() != null && submission.getDueDate() != null &&
                            u.getTurnedInAt().atZone(ZoneOffset.UTC).toLocalDate().isAfter(submission.getDueDate());
                    return StudentSubmissionResponse.builder()
                            .studentId(s.getId())
                            .studentName(s.getFirstName() + " " + s.getLastName())
                            .studentRef(s.getStudentRef())
                            .fileName(u.getOriginalFileName())
                            .submittedAt(u.getTurnedInAt().toString())
                            .attemptNumber((int) attempts)
                            .late(late)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public MyUploadStatusResponse getMyUploadStatus(Long submissionId, String studentEmail) {
        Student student = studentRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));
        return uploadRepository
                .findTopBySubmissionIdAndStudentIdOrderByUploadedAtDesc(submissionId, student.getId())
                .map(u -> MyUploadStatusResponse.builder()
                        .uploadId(u.getId())
                        .turnedIn(Boolean.TRUE.equals(u.getTurnedIn()))
                        .fileName(u.getOriginalFileName())
                        .turnedInAt(u.getTurnedInAt() != null ? u.getTurnedInAt().toString() : null)
                        .compliancePassed(u.isCompliancePassed())
                        .build())
                .orElse(null);
    }

    private String saveFile(MultipartFile file, Long submissionId, Long studentId, String ext) throws IOException {
        Path dir = Paths.get(uploadDir, String.valueOf(submissionId), String.valueOf(studentId));
        Files.createDirectories(dir);
        Path target = dir.resolve(UUID.randomUUID() + "." + ext.toLowerCase());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        return target.toAbsolutePath().toString();
    }

    private String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot >= 0 ? fileName.substring(dot + 1) : "";
    }
}
