package fr.epita.controller;

import fr.epita.dto.Request.SendAnnouncementRequest;
import fr.epita.dto.Response.AnnouncementResponse;
import fr.epita.dto.Response.SentAnnouncementResponse;
import fr.epita.enums.Role;
import fr.epita.model.AppUser;
import fr.epita.service.AnnouncementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    // ── SEND (Lecturer + UNI_ADMIN) ──────────────────────────────────────

    @PostMapping
    public ResponseEntity<Void> send(
            @Valid @RequestBody SendAnnouncementRequest request,
            @AuthenticationPrincipal AppUser currentUser) {
        announcementService.send(request, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // ── SENT OUTBOX (UNI_ADMIN + Lecturer) ──────────────────────────────

    @GetMapping("/sent")
    public ResponseEntity<List<SentAnnouncementResponse>> getSent(
            @AuthenticationPrincipal AppUser currentUser) {
        return ResponseEntity.ok(announcementService.getSent(currentUser));
    }

    // ── STUDENT INBOX ────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<List<AnnouncementResponse>> getMyInbox(
            @AuthenticationPrincipal AppUser currentUser) {
        if (currentUser.getRole() == Role.ROLE_STUDENT) {
            return ResponseEntity.ok(announcementService.getInboxForStudent(currentUser));
        }
        if (currentUser.getRole() == Role.ROLE_LECTURER) {
            return ResponseEntity.ok(announcementService.getInboxForLecturer(currentUser));
        }
        return ResponseEntity.ok(List.of());
    }

    @GetMapping("/me/unread-count")
    public ResponseEntity<Long> getMyUnreadCount(
            @AuthenticationPrincipal AppUser currentUser) {
        if (currentUser.getRole() == Role.ROLE_STUDENT) {
            return ResponseEntity.ok(announcementService.unreadCountForStudent(currentUser));
        }
        if (currentUser.getRole() == Role.ROLE_LECTURER) {
            return ResponseEntity.ok(announcementService.unreadCountForLecturer(currentUser));
        }
        return ResponseEntity.ok(0L);
    }

    @PatchMapping("/me/read-all")
    public ResponseEntity<Void> markAllRead(
            @AuthenticationPrincipal AppUser currentUser) {
        if (currentUser.getRole() == Role.ROLE_STUDENT) {
            announcementService.markAllStudentRead(currentUser);
        } else if (currentUser.getRole() == Role.ROLE_LECTURER) {
            announcementService.markAllLecturerRead(currentUser);
        }
        return ResponseEntity.ok().build();
    }

    // ── MARK SINGLE AS READ ──────────────────────────────────────────────

    @PatchMapping("/student-recipients/{recipientId}/read")
    public ResponseEntity<Void> markStudentRead(@PathVariable Long recipientId) {
        announcementService.markStudentRead(recipientId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/lecturer-recipients/{recipientId}/read")
    public ResponseEntity<Void> markLecturerRead(@PathVariable Long recipientId) {
        announcementService.markLecturerRead(recipientId);
        return ResponseEntity.ok().build();
    }
}
