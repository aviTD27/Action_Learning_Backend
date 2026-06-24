package fr.epita.service;

import fr.epita.dto.Request.CreatePlatformAdminRequest;
import fr.epita.dto.Response.PlatformAdminResponse;
import fr.epita.enums.Role;
import fr.epita.model.AppUser;
import fr.epita.repository.AppUserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SuperAdminService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$!";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Returns all non-deleted admin users (university admins + platform admins). */
    public List<PlatformAdminResponse> listPlatformAdmins() {
        return appUserRepository.findByRoleInAndDeletedFalse(
                        List.of(Role.ROLE_UNI_ADMIN, Role.ROLE_PLATFORM_ADMIN))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Creates a new platform admin and emails their temporary credentials. */
    public PlatformAdminResponse createPlatformAdmin(CreatePlatformAdminRequest req) {
        String email = req.getEmail().trim().toLowerCase();
        if (appUserRepository.existsByEmail(email)) {
            throw new IllegalStateException("An account with this email already exists.");
        }

        String tempPassword = generateTempPassword();

        AppUser admin = AppUser.builder()
                .firstName(req.getFirstName().trim())
                .surname(req.getSurname().trim())
                .email(email)
                .password(passwordEncoder.encode(tempPassword))
                .role(Role.ROLE_PLATFORM_ADMIN)
                .build();

        appUserRepository.save(admin);
        emailService.sendPlatformAdminWelcomeEmail(email, req.getFirstName().trim(), tempPassword);
        return toResponse(admin);
    }

    /** Blocks a platform admin — they can no longer log in. */
    public PlatformAdminResponse blockPlatformAdmin(Long id) {
        AppUser admin = findActive(id);
        admin.setBlocked(true);
        return toResponse(appUserRepository.save(admin));
    }

    /** Unblocks a platform admin — restores login access. */
    public PlatformAdminResponse unblockPlatformAdmin(Long id) {
        AppUser admin = findActive(id);
        admin.setBlocked(false);
        return toResponse(appUserRepository.save(admin));
    }

    /**
     * Soft-deletes a platform admin.
     * Sets deleted = true so they are hidden from the UI but kept in the database.
     * Only a developer can restore them via the database.
     */
    public void softDeletePlatformAdmin(Long id) {
        AppUser admin = findActive(id);
        admin.setDeleted(true);
        admin.setBlocked(true);
        appUserRepository.save(admin);
    }

    private AppUser findActive(Long id) {
        return appUserRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new EntityNotFoundException("Platform admin not found or already deleted."));
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private PlatformAdminResponse toResponse(AppUser u) {
        return PlatformAdminResponse.builder()
                .id(u.getId())
                .firstName(u.getFirstName())
                .surname(u.getSurname())
                .email(u.getEmail())
                .role(u.getRole())
                .blocked(u.isBlocked())
                .deleted(u.isDeleted())
                .dateOfBirth(u.getDateOfBirth())
                .build();
    }
}
