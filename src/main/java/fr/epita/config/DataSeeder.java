package fr.epita.config;

import fr.epita.enums.Role;
import fr.epita.model.AppUser;
import fr.epita.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (appUserRepository.existsByRole(Role.ROLE_SUPER_ADMIN)) {
            return;
        }

        AppUser superAdmin = AppUser.builder()
                .firstName("Super")
                .surname("Admin")
                .email("superadmin@acl.com")
                .password(passwordEncoder.encode("Admin@1234"))
                .role(Role.ROLE_SUPER_ADMIN)
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();

        appUserRepository.save(superAdmin);
        System.out.println("=== Super Admin seeded: superadmin@acl.com / Admin@1234 ===");
    }
}
