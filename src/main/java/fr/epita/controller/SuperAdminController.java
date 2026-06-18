package fr.epita.controller;

import fr.epita.dto.Request.CreatePlatformAdminRequest;
import fr.epita.dto.Response.PlatformAdminResponse;
import fr.epita.service.SuperAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/super-admin")
@RequiredArgsConstructor
public class SuperAdminController {

    private final SuperAdminService superAdminService;

    @GetMapping("/platform-admins")
    public ResponseEntity<List<PlatformAdminResponse>> list() {
        return ResponseEntity.ok(superAdminService.listPlatformAdmins());
    }

    @PostMapping("/platform-admins")
    public ResponseEntity<PlatformAdminResponse> create(@Valid @RequestBody CreatePlatformAdminRequest req) {
        return ResponseEntity.ok(superAdminService.createPlatformAdmin(req));
    }

    @PatchMapping("/platform-admins/{id}/block")
    public ResponseEntity<PlatformAdminResponse> block(@PathVariable Long id) {
        return ResponseEntity.ok(superAdminService.blockPlatformAdmin(id));
    }

    @PatchMapping("/platform-admins/{id}/unblock")
    public ResponseEntity<PlatformAdminResponse> unblock(@PathVariable Long id) {
        return ResponseEntity.ok(superAdminService.unblockPlatformAdmin(id));
    }

    @DeleteMapping("/platform-admins/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        superAdminService.softDeletePlatformAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
