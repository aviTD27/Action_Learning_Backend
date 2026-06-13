package fr.epita.service;

import fr.epita.dto.Request.LoginRequest;
import fr.epita.dto.Request.RegisterRequest;
import fr.epita.dto.Response.AuthResponse;
import fr.epita.enums.Role;
import fr.epita.model.AppUser;
import fr.epita.repository.AppUserRepository;
import fr.epita.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (appUserRepository.existsByEmail(request.getEmail())) {
            throw new IllegalStateException("Email already registered: " + request.getEmail());
        }

        Role assignedRole = resolveRole(request.getRole());

        AppUser user = AppUser.builder()
                .firstName(request.getFirstName())
                .surname(request.getSurname())
                .dateOfBirth(request.getDateOfBirth())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(assignedRole)
                .build();

        appUserRepository.save(user);

        return new AuthResponse(jwtUtil.generateToken(user.getEmail(), user.getRole()));
    }

    public AuthResponse login(LoginRequest request) {
        AppUser user = appUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        return new AuthResponse(jwtUtil.generateToken(user.getEmail(), user.getRole()));
    }

    /**
     * Bootstrap rule:
     *   - No valid JWT → caller is anonymous → only ROLE_ADMIN may be provisioned.
     *   - Valid JWT with ROLE_ADMIN → any role the request specifies (defaults to ROLE_STUDENT).
     */
    private Role resolveRole(Role requested) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean callerIsAdmin = auth != null
                && auth.isAuthenticated()
                && auth.getPrincipal() instanceof AppUser
                && auth.getAuthorities().stream()
                       .anyMatch(a -> a.getAuthority().equals(Role.ROLE_ADMIN.name()));

        if (callerIsAdmin) {
            return requested != null ? requested : Role.ROLE_STUDENT;
        }

        // Unauthenticated: bootstrap path — only ROLE_ADMIN is permitted
        if (requested != null && requested != Role.ROLE_ADMIN) {
            throw new IllegalStateException("Only ROLE_ADMIN can be registered without authentication");
        }
        return Role.ROLE_ADMIN;
    }
}
