package fr.epita.controller;

import fr.epita.dto.Request.CreateRegistrationRequest;
import fr.epita.dto.Request.DeclineRegistrationRequest;
import fr.epita.dto.Response.RegistrationResponse;
import fr.epita.enums.RegistrationStatus;
import fr.epita.service.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registrations")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping
    public ResponseEntity<RegistrationResponse> submit(@Valid @RequestBody CreateRegistrationRequest request) {
        return ResponseEntity.ok(registrationService.submit(request));
    }

    @GetMapping
    public ResponseEntity<List<RegistrationResponse>> getAll(@RequestParam(required = false) RegistrationStatus status) {
        return ResponseEntity.ok(registrationService.getAll(status));
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<RegistrationResponse> approve(@PathVariable Long id) {
        return ResponseEntity.ok(registrationService.approve(id));
    }

    @PatchMapping("/{id}/decline")
    public ResponseEntity<RegistrationResponse> decline(@PathVariable Long id, @RequestBody(required = false) DeclineRegistrationRequest body) {
        return ResponseEntity.ok(registrationService.decline(id, body != null ? body.getReason() : null));
    }
}
