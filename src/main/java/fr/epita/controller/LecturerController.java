package fr.epita.controller;

import fr.epita.dto.Request.CreateLecturerRequest;
import fr.epita.dto.Response.LecturerResponse;
import fr.epita.service.LecturerService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lecturers")
@RequiredArgsConstructor
public class LecturerController {

    private final LecturerService lecturerService;

    @GetMapping
    public ResponseEntity<List<LecturerResponse>> getAll() {
        return ResponseEntity.ok(lecturerService.getAll());
    }

    @PostMapping
    public ResponseEntity<LecturerResponse> create(@Valid @RequestBody CreateLecturerRequest request) {
        return ResponseEntity.ok(lecturerService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LecturerResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateLecturerRequest request) {
        return ResponseEntity.ok(lecturerService.update(id, request));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<Void> archive(@PathVariable Long id) {
        lecturerService.archive(id);
        return ResponseEntity.ok().build();
    }
}
