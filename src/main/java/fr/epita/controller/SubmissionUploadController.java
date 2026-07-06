package fr.epita.controller;

import fr.epita.dto.Response.ComplianceReportResponse;
import fr.epita.dto.Response.MyUploadStatusResponse;
import fr.epita.dto.Response.ScoringReportResponse;
import fr.epita.dto.Response.StudentSubmissionResponse;
import fr.epita.enums.Role;
import fr.epita.model.AppUser;
import fr.epita.model.Student;
import fr.epita.model.Submission;
import fr.epita.model.SubmissionUpload;
import fr.epita.repository.StudentRepository;
import fr.epita.repository.SubmissionRepository;
import fr.epita.repository.SubmissionUploadRepository;
import fr.epita.service.SubmissionUploadService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SubmissionUploadController {

    private final SubmissionRepository submissionRepository;
    private final StudentRepository studentRepository;
    private final SubmissionUploadService uploadService;
    private final SubmissionUploadRepository uploadRepository;

    @PostMapping(value = "/{submissionId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ComplianceReportResponse> uploadDocument(
            @PathVariable Long submissionId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal AppUser currentUser) throws IOException {

        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found"));

        Student student = studentRepository.findByEmail(currentUser.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        return ResponseEntity.ok(uploadService.processUpload(submission, student, file));
    }

    @PatchMapping("/uploads/{uploadId}/turn-in")
    public ResponseEntity<Void> turnIn(
            @PathVariable Long uploadId,
            @AuthenticationPrincipal AppUser currentUser) {
        uploadService.turnIn(uploadId, currentUser.getEmail());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{submissionId}/student-submissions")
    public ResponseEntity<List<StudentSubmissionResponse>> getStudentSubmissions(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal AppUser currentUser) {
        if (currentUser == null || currentUser.getRole() == Role.ROLE_STUDENT) {
            return ResponseEntity.status(403).build();
        }
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found"));
        return ResponseEntity.ok(uploadService.getStudentSubmissions(submission));
    }

    @GetMapping("/{submissionId}/my-upload")
    public ResponseEntity<MyUploadStatusResponse> getMyUploadStatus(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal AppUser currentUser) {
        MyUploadStatusResponse status = uploadService.getMyUploadStatus(submissionId, currentUser.getEmail());
        if (status == null) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(status);
    }

    /** Row 68 — lecturer uploads a template/brief file for the assignment. */
    @PostMapping(value = "/{submissionId}/template", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadTemplate(
            @PathVariable Long submissionId,
            @RequestParam("file") MultipartFile file) throws IOException {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found"));
        uploadService.storeTemplate(submission, file);
        return ResponseEntity.ok().build();
    }

    /** Row 68 — anyone in the cohort (and the lecturer) downloads the template file. */
    @GetMapping("/{submissionId}/template")
    public ResponseEntity<byte[]> downloadTemplate(@PathVariable Long submissionId) throws IOException {
        Submission submission = submissionRepository.findById(submissionId)
                .orElseThrow(() -> new EntityNotFoundException("Submission not found"));
        if (submission.getTemplateStoredPath() == null || submission.getTemplateStoredPath().isBlank()) {
            return ResponseEntity.noContent().build();
        }
        byte[] data = uploadService.readBytes(submission.getTemplateStoredPath());
        return fileResponse(data, submission.getTemplateFileName());
    }

    /** Row 75 — lecturer downloads a single student's submitted file. Students can only download their own. */
    @GetMapping("/uploads/{uploadId}/download")
    public ResponseEntity<byte[]> downloadUpload(
            @PathVariable Long uploadId,
            @AuthenticationPrincipal AppUser currentUser) throws IOException {
        SubmissionUpload upload = uploadService.getUploadForDownload(uploadId);
        if (currentUser != null && currentUser.getRole() == Role.ROLE_STUDENT
                && !upload.getStudent().getEmail().equals(currentUser.getEmail())) {
            return ResponseEntity.status(403).build();
        }
        if (upload.getStoredPath() == null || upload.getStoredPath().isBlank()) {
            return ResponseEntity.noContent().build();
        }
        byte[] data = uploadService.readBytes(upload.getStoredPath());
        return fileResponse(data, upload.getOriginalFileName());
    }

    /** Returns the NLP scoring breakdown for an upload — lecturer and uni-admin only. */
    @GetMapping("/uploads/{uploadId}/score")
    public ResponseEntity<ScoringReportResponse> getScore(
            @PathVariable Long uploadId,
            @AuthenticationPrincipal AppUser currentUser) {
        if (currentUser == null || currentUser.getRole() == Role.ROLE_STUDENT) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(uploadService.getScore(uploadId));
    }

    /** Row 76 — lecturer downloads every submission for an assignment as one ZIP. */
    @GetMapping("/{submissionId}/download-zip")
    public ResponseEntity<byte[]> downloadZip(
            @PathVariable Long submissionId,
            @AuthenticationPrincipal AppUser currentUser) throws IOException {
        if (currentUser == null || currentUser.getRole() == Role.ROLE_STUDENT) {
            return ResponseEntity.status(403).build();
        }
        byte[] zip = uploadService.buildSubmissionsZip(submissionId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"submissions-" + submissionId + ".zip\"")
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(zip);
    }

    private ResponseEntity<byte[]> fileResponse(byte[] data, String filename) {
        String safe = filename != null ? filename : "download";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + safe + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(data);
    }
}
