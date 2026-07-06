package fr.epita.controller;

import fr.epita.dto.Request.CreateTimetableRequest;
import fr.epita.dto.Response.TimetableResponse;
import fr.epita.model.AppUser;
import fr.epita.service.TimetableService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timetable")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    // All roles: flat list, filtered by role inside the service
    @GetMapping
    public ResponseEntity<List<TimetableResponse>> getAll(
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(timetableService.getAll(currentUser));
    }

    /**
     * Weekly view: entries grouped by day of week, sorted by startTime.
     * Multiple entries at the same time appear as a list under the same day key.
     * Only days that have at least one entry are included.
     */
    @GetMapping("/weekly")
    public ResponseEntity<Map<String, List<TimetableResponse>>> getWeekly(
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(timetableService.getWeekly(currentUser));
    }

    // UNI_ADMIN only
    @PostMapping
    public ResponseEntity<TimetableResponse> create(
            @Valid @RequestBody CreateTimetableRequest request,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timetableService.create(request, currentUser));
    }

    // UNI_ADMIN only
    @PutMapping("/{id}")
    public ResponseEntity<TimetableResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateTimetableRequest request,
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(timetableService.update(id, request, currentUser));
    }

    // UNI_ADMIN only
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal AppUser currentUser) {
        timetableService.delete(id, currentUser);
        return ResponseEntity.noContent().build();
    }
}
