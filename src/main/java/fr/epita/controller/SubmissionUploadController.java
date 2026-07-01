package fr.epita.controller;

import fr.epita.dto.Response.ComplianceReportResponse;
import fr.epita.dto.Response.MyUploadStatusResponse;
import fr.epita.dto.Response.StudentSubmissionResponse;
import fr.epita.model.AppUser;
import fr.epita.model.Student;
import fr.epita.model.Submission;
import fr.epita.repository.StudentRepository;
import fr.epita.repository.SubmissionRepository;
import fr.epita.service.SubmissionUploadService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SubmissionUploadController {

    private final SubmissionRepository submissionRepository;
    private final StudentRepository studentRepository;
    private final SubmissionUploadService uploadService;

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
            @PathVariable Long submissionId) {
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
}
