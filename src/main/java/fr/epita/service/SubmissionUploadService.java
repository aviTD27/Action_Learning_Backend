package fr.epita.service;

import fr.epita.dto.Response.ComplianceReportResponse;
import fr.epita.dto.Response.MyUploadStatusResponse;
import fr.epita.dto.Response.StudentSubmissionResponse;
import fr.epita.model.Student;
import fr.epita.model.Submission;
import fr.epita.model.SubmissionUpload;
import fr.epita.repository.StudentRepository;
import fr.epita.repository.SubmissionRepository;
import fr.epita.repository.SubmissionUploadRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class SubmissionUploadService {

    private final AiServiceClient aiServiceClient;
    private final SubmissionUploadRepository uploadRepository;
    private final SubmissionRepository submissionRepository;
    private final StudentRepository studentRepository;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Transactional
    public ComplianceReportResponse processUpload(
            Submission submission, Student student, MultipartFile file) throws IOException {

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown";

        // Enforce the maximum file size (row 64) before anything else.
        if (submission.getRules() != null
                && submission.getRules().getMaxFileSizeBytes() != null
                && file.getSize() > submission.getRules().getMaxFileSizeBytes()) {
            long limitMb = submission.getRules().getMaxFileSizeBytes() / (1024 * 1024);
            throw new IllegalStateException(
                    "File is too large. The maximum allowed size for this assignment is " + limitMb + " MB.");
        }

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

    /**
     * Row 74/75 — the turned-in submissions for an assignment, each enriched with
     * status (SUBMITTED/LATE), the uploadId for download, and the re-open flag.
     * Students who have not submitted are derived on the client from the cohort roster.
     */
    public List<StudentSubmissionResponse> getStudentSubmissions(Submission submission) {
        var reopened = submission.getReopenedStudentIds() != null
                ? submission.getReopenedStudentIds() : java.util.Set.<Long>of();

        return uploadRepository.findBySubmissionIdAndTurnedInTrue(submission.getId())
                .stream()
                .map(u -> {
                    Student s = u.getStudent();
                    long attempts = uploadRepository.countBySubmissionIdAndStudentId(submission.getId(), s.getId());
                    boolean late = u.getTurnedInAt() != null
                            && u.getTurnedInAt().atZone(ZoneOffset.UTC).toLocalDateTime()
                                    .isAfter(submission.deadline());
                    return StudentSubmissionResponse.builder()
                            .studentId(s.getId())
                            .studentName(s.getFirstName() + " " + s.getLastName())
                            .studentRef(s.getStudentRef())
                            .studentEmail(s.getEmail())
                            .status(late ? "LATE" : "SUBMITTED")
                            .uploadId(u.getId())
                            .fileName(u.getOriginalFileName())
                            .submittedAt(u.getTurnedInAt() != null ? u.getTurnedInAt().toString() : null)
                            .attemptNumber((int) attempts)
                            .late(late)
                            .reopened(reopened.contains(s.getId()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    /** Loads a single upload (for a lecturer download); row 75. */
    public SubmissionUpload getUploadForDownload(Long uploadId) {
        return uploadRepository.findById(uploadId)
                .orElseThrow(() -> new EntityNotFoundException("Upload not found"));
    }

    /** All turned-in uploads for a submission (used to build the ZIP); row 76. */
    public List<SubmissionUpload> getTurnedInUploads(Long submissionId) {
        return uploadRepository.findBySubmissionIdAndTurnedInTrue(submissionId);
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

    /** Stores a template/brief file for an assignment and records it on the submission (row 68). */
    @Transactional
    public void storeTemplate(Submission submission, MultipartFile file) throws IOException {
        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "template";
        Path dir = Paths.get(uploadDir, "templates", String.valueOf(submission.getId()));
        Files.createDirectories(dir);
        String ext = getExtension(originalName);
        Path target = dir.resolve(UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext.toLowerCase()));
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        submission.setTemplateFileName(originalName);
        submission.setTemplateStoredPath(target.toAbsolutePath().toString());
        submissionRepository.save(submission);
    }

    /** Reads raw bytes from a stored path (template or a student upload). */
    public byte[] readBytes(String storedPath) throws IOException {
        return Files.readAllBytes(Paths.get(storedPath));
    }

    /** Builds a ZIP of every turned-in submission file for an assignment (row 76). */
    public byte[] buildSubmissionsZip(Long submissionId) throws IOException {
        List<SubmissionUpload> uploads = uploadRepository.findBySubmissionIdAndTurnedInTrue(submissionId);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (SubmissionUpload u : uploads) {
                if (u.getStoredPath() == null || u.getStoredPath().isBlank()) continue;
                Path p = Paths.get(u.getStoredPath());
                if (!Files.exists(p)) continue;
                Student s = u.getStudent();
                String ref = s.getStudentRef() != null ? s.getStudentRef() : ("student" + s.getId());
                String entryName = ref + "_" + u.getOriginalFileName();
                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(Files.readAllBytes(p));
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
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
