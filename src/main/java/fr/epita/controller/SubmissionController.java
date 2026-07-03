package fr.epita.controller;

import fr.epita.dto.Request.CreateSubmissionRequest;
import fr.epita.dto.Request.GradeRequest;
import fr.epita.dto.Response.GradeResponse;
import fr.epita.dto.Response.SubmissionResponse;
import fr.epita.enums.Role;
import fr.epita.model.AppUser;
import fr.epita.service.GradeService;
import fr.epita.service.SubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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
        boolean studentView = currentUser != null && currentUser.getRole() == Role.ROLE_STUDENT;
        return ResponseEntity.ok(submissionService.getAll(cohortId, lecturerId, universityId, studentView));
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

    @PatchMapping("/{id}/notify")
    public ResponseEntity<SubmissionResponse> notifyStudents(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.notifyStudents(id));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<SubmissionResponse> publish(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.publish(id));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<SubmissionResponse> archive(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.archive(id));
    }

    @PatchMapping("/{id}/unarchive")
    public ResponseEntity<SubmissionResponse> unarchive(@PathVariable Long id) {
        return ResponseEntity.ok(submissionService.unarchive(id));
    }

    @PatchMapping("/{id}/reopen/{studentId}")
    public ResponseEntity<SubmissionResponse> reopen(
            @PathVariable Long id, @PathVariable Long studentId) {
        return ResponseEntity.ok(submissionService.reopenForStudent(id, studentId));
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
