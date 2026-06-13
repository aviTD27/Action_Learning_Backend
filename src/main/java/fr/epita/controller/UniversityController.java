package fr.epita.controller;

import fr.epita.dto.Request.CreateUniversityRequest;
import fr.epita.dto.Response.UniversityResponse;
import fr.epita.service.UniversityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/universities")
@RequiredArgsConstructor
public class UniversityController {

    private final UniversityService universityService;

    @GetMapping
    public ResponseEntity<List<UniversityResponse>> getAll() {
        return ResponseEntity.ok(universityService.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UniversityResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(universityService.getById(id));
    }

    @PostMapping
    public ResponseEntity<UniversityResponse> create(@Valid @RequestBody CreateUniversityRequest request) {
        return ResponseEntity.ok(universityService.create(request));
    }
}
