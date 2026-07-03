package fr.epita.controller;

import fr.epita.dto.Request.CreateSubmissionRequest;
import fr.epita.dto.Request.GradeRequest;
import fr.epita.dto.Response.GradeResponse;
import fr.epita.dto.Response.SubmissionResponse;
import fr.epita.model.AppUser;
import fr.epita.service.GradeService;
import fr.epita.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final GradeService gradeService;

    @GetMapping
    public ResponseEntity<List<SubmissionResponse>> getAll(
            @RequestParam(required = false) Long cohortId,
            @RequestParam(required = false) Long lecturerId,
            @AuthenticationPrincipal AppUser currentUser) {
        Long universityId = currentUser != null ? currentUser.getUniversityId() : null;
        return ResponseEntity.ok(submissionService.getAll(cohortId, lecturerId, universityId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SubmissionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SubmissionResponse> create(@Valid @RequestBody CreateSubmissionRequest request) {
        return ResponseEntity.ok(submissionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SubmissionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateSubmissionRequest request) {
        return ResponseEntity.ok(submissionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        submissionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/template", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<SubmissionResponse> uploadTemplate(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(submissionService.saveTemplate(id, file));
    }

    @GetMapping("/{id}/template/download")
    public ResponseEntity<Resource> downloadTemplate(@PathVariable Long id) {
        return submissionService.downloadTemplate(id);
    }

    /** TODO: Send Email */
    @PatchMapping("/{id}/notify")
    public ResponseEntity<SubmissionResponse> notifyStudents(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.notifyStudents(id));
    }

    //  Grades

    @GetMapping("/{id}/grades")
    public ResponseEntity<List<GradeResponse>> getGrades(@PathVariable Long id) {
        return ResponseEntity.ok(gradeService.getForSubmission(id));
    }

    @PutMapping("/{id}/grades/{studentId}")
    public ResponseEntity<GradeResponse> setGrade(
            @PathVariable Long id,
            @PathVariable Long studentId,
            @Valid @RequestBody GradeRequest request) {
        return ResponseEntity.ok(gradeService.setGrade(id, studentId, request));
    }

    @PatchMapping("/{id}/grades/release")
    public ResponseEntity<List<GradeResponse>> releaseGrades(@PathVariable Long id) {
        return ResponseEntity.ok(gradeService.releaseAll(id));
    }
}
