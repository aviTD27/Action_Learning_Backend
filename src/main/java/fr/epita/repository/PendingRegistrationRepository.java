package fr.epita.repository;

import fr.epita.enums.RegistrationStatus;
import fr.epita.model.PendingRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {
    List<PendingRegistration> findByStatusOrderBySubmittedAtDesc(RegistrationStatus status);
    List<PendingRegistration> findAllByOrderBySubmittedAtDesc();
    boolean existsByDomainIgnoreCaseAndStatus(String domain, RegistrationStatus status);
    boolean existsByAdminContactEmailIgnoreCaseAndStatus(String email, RegistrationStatus status);
    Optional<PendingRegistration> findFirstByOrgNameIgnoreCaseAndStatus(String orgName, RegistrationStatus status);
}
