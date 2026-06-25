package fr.epita.repository;

import fr.epita.enums.Role;
import fr.epita.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRole(Role role);

    /** All non-deleted users with the given role (used by SuperAdminService). */
    List<AppUser> findByRoleAndDeletedFalse(Role role);

    /** All non-deleted users whose role is in the given list (used to list all admin users). */
    List<AppUser> findByRoleInAndDeletedFalse(List<Role> roles);

    /** Fetch a single non-deleted user by id (used for block/unblock/delete). */
    Optional<AppUser> findByIdAndDeletedFalse(Long id);

    /** Find the first admin user for a given university (used to derive the university email domain). */
    Optional<AppUser> findFirstByUniversityIdAndRole(Long universityId, Role role);
}
