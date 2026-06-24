package fr.epita.model;

import fr.epita.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String surname;

    @Column(nullable = true)
    private LocalDate dateOfBirth;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "VARCHAR(50)")
    private Role role;

    /** false = account can log in; true = login blocked by admin */
    @Builder.Default
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean blocked = false;

    /** false = visible and active; true = soft-deleted (hidden from UI, preserved in DB) */
    @Builder.Default
    @ColumnDefault("false")
    @Column(nullable = false)
    private boolean deleted = false;

    /** Null for SUPER_ADMIN and PLATFORM_ADMIN; set for ROLE_UNI_ADMIN, ROLE_LECTURER, ROLE_STUDENT */
    @Column(name = "university_id")
    private Long universityId;
}
