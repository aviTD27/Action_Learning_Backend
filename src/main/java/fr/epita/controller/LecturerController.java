package fr.epita.controller;

import fr.epita.dto.Request.CreateLecturerRequest;
import fr.epita.dto.Response.LecturerResponse;
import fr.epita.model.AppUser;
import fr.epita.service.LecturerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lecturers")
@RequiredArgsConstructor
public class LecturerController {

    private final LecturerService lecturerService;

    @GetMapping
    public ResponseEntity<List<LecturerResponse>> getAll(
            @RequestParam(required = false) Long universityId) {
        return ResponseEntity.ok(lecturerService.getAll(universityId));
    }

    @PostMapping
    public ResponseEntity<LecturerResponse> create(
            @Valid @RequestBody CreateLecturerRequest request,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(lecturerService.create(request, currentUser.getUniversityId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LecturerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateLecturerRequest request) {
        return ResponseEntity.ok(lecturerService.update(id, request));
    }
    
}
