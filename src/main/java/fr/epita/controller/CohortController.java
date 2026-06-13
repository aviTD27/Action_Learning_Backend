package fr.epita.controller;

import fr.epita.dto.Response.CohortResponse;
import fr.epita.dto.Request.CreateCohortRequest;
import fr.epita.service.CohortService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cohorts")
@RequiredArgsConstructor
public class CohortController {

    private final CohortService cohortService;

    @GetMapping
    public ResponseEntity<List<CohortResponse>> getAll(
            @RequestParam(required = false) Long universityId) {
        return ResponseEntity.ok(cohortService.getAll(universityId));
    }

    @PostMapping
    public ResponseEntity<CohortResponse> create(@Valid @RequestBody CreateCohortRequest request) {
        return ResponseEntity.ok(cohortService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CohortResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateCohortRequest request) {
        return ResponseEntity.ok(cohortService.update(id, request));
    }
}

