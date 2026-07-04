package fr.epita.controller;

import fr.epita.dto.Request.CreateCourseRequest;
import fr.epita.dto.Response.CourseResponse;
import fr.epita.model.AppUser;
import fr.epita.service.CourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public ResponseEntity<List<CourseResponse>> getAll(
            @RequestParam(required = false) Long semesterId,
            @RequestParam(required = false) Long programmeId,
            @RequestParam(required = false) Long lecturerId,
            @RequestParam(required = false) Long universityId,
            @AuthenticationPrincipal AppUser currentUser) {
        Long uni = universityId != null ? universityId
                : (currentUser != null ? currentUser.getUniversityId() : null);
        return ResponseEntity.ok(courseService.getAll(semesterId, programmeId, lecturerId, uni));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getById(id));
    }

    @PostMapping
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.ok(courseService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CourseResponse> update(
            @PathVariable Long id, @Valid @RequestBody CreateCourseRequest request) {
        return ResponseEntity.ok(courseService.update(id, request));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<CourseResponse> archive(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.archive(id));
    }

    @PatchMapping("/{id}/unarchive")
    public ResponseEntity<CourseResponse> unarchive(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.unarchive(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        courseService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
