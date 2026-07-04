package fr.epita.controller;

import fr.epita.dto.Response.CohortResponse;
import fr.epita.dto.Request.CreateCohortRequest;
import fr.epita.model.AppUser;
import fr.epita.service.CohortService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cohorts")
@RequiredArgsConstructor
public class CohortController {

    private final CohortService cohortService;

    @GetMapping
    public ResponseEntity<List<CohortResponse>> getAll(
            @RequestParam(required = false) Long universityId,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(cohortService.getAll(resolve(universityId, currentUser)));
    }

    @PostMapping
    public ResponseEntity<CohortResponse> create(
            @Valid @RequestBody CreateCohortRequest request,
            @RequestParam(required = false) Long universityId,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(cohortService.create(request, resolve(universityId, currentUser)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CohortResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateCohortRequest request) {
        return ResponseEntity.ok(cohortService.update(id, request));
    }

    private Long resolve(Long universityId, AppUser currentUser) {
        if (universityId != null) return universityId;
        return currentUser != null ? currentUser.getUniversityId() : null;
    }
}

