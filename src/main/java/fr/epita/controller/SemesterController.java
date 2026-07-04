package fr.epita.controller;

import fr.epita.dto.Request.CreateSemesterRequest;
import fr.epita.dto.Response.SemesterResponse;
import fr.epita.model.AppUser;
import fr.epita.service.SemesterService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/semesters")
@RequiredArgsConstructor
public class SemesterController {

    private final SemesterService semesterService;

    @GetMapping
    public ResponseEntity<List<SemesterResponse>> getAll(
            @RequestParam(required = false) Long programmeId,
            @RequestParam(required = false) Long universityId,
            @AuthenticationPrincipal AppUser currentUser) {
        Long uni = universityId != null ? universityId
                : (currentUser != null ? currentUser.getUniversityId() : null);
        return ResponseEntity.ok(semesterService.getAll(programmeId, uni));
    }

    @PostMapping
    public ResponseEntity<SemesterResponse> create(@Valid @RequestBody CreateSemesterRequest request) {
        return ResponseEntity.ok(semesterService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SemesterResponse> update(
            @PathVariable Long id, @Valid @RequestBody CreateSemesterRequest request) {
        return ResponseEntity.ok(semesterService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        semesterService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
