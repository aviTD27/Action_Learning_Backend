package fr.epita.service;

import fr.epita.dto.Request.CreateRegistrationRequest;
import fr.epita.dto.Response.RegistrationResponse;
import fr.epita.enums.RegistrationStatus;
import fr.epita.enums.Role;
import fr.epita.model.AppUser;
import fr.epita.model.PendingRegistration;
import fr.epita.model.University;
import fr.epita.repository.AppUserRepository;
import fr.epita.repository.PendingRegistrationRepository;
import fr.epita.repository.UniversityRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final PendingRegistrationRepository registrationRepository;
    private final UniversityRepository universityRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final String PASSWORD_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$!";
    private static final SecureRandom RANDOM = new SecureRandom();

    /** Public: a visitor submits an access request from the landing page. */
    public RegistrationResponse submit(CreateRegistrationRequest request) {
        String domain = request.getDomain().trim().toLowerCase();
        String email = request.getAdminContactEmail().trim().toLowerCase();

        if (registrationRepository.existsByDomainIgnoreCaseAndStatus(domain, RegistrationStatus.PENDING)
                || registrationRepository.existsByDomainIgnoreCaseAndStatus(domain, RegistrationStatus.APPROVED))
            throw new IllegalStateException("A request for this domain already exists");
        if (registrationRepository.existsByAdminContactEmailIgnoreCaseAndStatus(email, RegistrationStatus.PENDING))
            throw new IllegalStateException("A pending request with this email already exists");

        PendingRegistration reg = PendingRegistration.builder()
                .orgName(request.getOrgName().trim())
                .country(request.getCountry())
                .websiteUrl(request.getWebsiteUrl())
                .domain(domain)
                .adminFirstName(request.getAdminFirstName())
                .adminLastName(request.getAdminLastName())
                .adminContactEmail(email)
                .adminPhone(request.getAdminPhone())
                .description(request.getDescription())
                .status(RegistrationStatus.PENDING)
                .build();

        return toResponse(registrationRepository.save(reg));
    }

    public List<RegistrationResponse> getAll(RegistrationStatus status) {
        List<PendingRegistration> rows = (status != null)
                ? registrationRepository.findByStatusOrderBySubmittedAtDesc(status)
                : registrationRepository.findAllByOrderBySubmittedAtDesc();
        return rows.stream().map(this::toResponse).toList();
    }

    /**
     * Approve: creates the University, provisions a ROLE_ADMIN AppUser whose login
     * email is generated as firstname.lastname@domain, then emails both the generated
     * email and the temp password to the requester's contact email.
     */
    @Transactional
    public RegistrationResponse approve(Long id) {
        PendingRegistration reg = find(id);
        if (reg.getStatus() != RegistrationStatus.PENDING)
            throw new IllegalStateException("Request is not pending");

        // 1. Create university
        String code = deriveCode(reg.getDomain(), reg.getOrgName());
        if (!universityRepository.existsByName(reg.getOrgName()) && !universityRepository.existsByCode(code)) {
            University uni = University.builder().name(reg.getOrgName()).code(code).build();
            universityRepository.save(uni);
        }

        // 2. Generate platform login email from name + domain  e.g. jordan.meye@epita.fr
        String platformEmail = generatePlatformEmail(reg.getAdminFirstName(), reg.getAdminLastName(), reg.getDomain());
        String tempPassword = generateTempPassword();

        log.info("Approving registration for {} — platform email: {}", reg.getAdminContactEmail(), platformEmail);

        if (!appUserRepository.existsByEmail(platformEmail)) {
            AppUser admin = AppUser.builder()
                    .firstName(reg.getAdminFirstName())
                    .surname(reg.getAdminLastName())
                    .email(platformEmail)
                    .password(passwordEncoder.encode(tempPassword))
                    .role(Role.ROLE_ADMIN)
                    .build();
            appUserRepository.save(admin);
        }

        // 3. Send credentials to the contact email (async — does not block the transaction)
        emailService.sendApprovalEmail(
                reg.getAdminContactEmail(),
                reg.getAdminFirstName(),
                platformEmail,
                tempPassword
        );

        reg.setStatus(RegistrationStatus.APPROVED);
        reg.setReviewedAt(Instant.now());
        return toResponse(registrationRepository.save(reg));
    }

    @Transactional
    public RegistrationResponse decline(Long id, String reason) {
        PendingRegistration reg = find(id);
        if (reg.getStatus() != RegistrationStatus.PENDING)
            throw new IllegalStateException("Request is not pending");

        reg.setStatus(RegistrationStatus.DECLINED);
        reg.setDeclineReason(reason);
        reg.setReviewedAt(Instant.now());
        PendingRegistration saved = registrationRepository.save(reg);

        // Notify the requester of the decision
        emailService.sendRejectionEmail(reg.getAdminContactEmail(), reg.getAdminFirstName(), reason);

        return toResponse(saved);
    }

    /**
     * Generates firstname.lastname@domain.
     * Appends a numeric suffix if that address already exists: firstname.lastname2@domain, etc.
     */
    private String generatePlatformEmail(String firstName, String lastName, String domain) {
        String local = sanitizeName(firstName) + "." + sanitizeName(lastName);
        String candidate = local + "@" + domain;
        if (!appUserRepository.existsByEmail(candidate)) return candidate;
        int n = 2;
        while (appUserRepository.existsByEmail(local + n + "@" + domain)) n++;
        return local + n + "@" + domain;
    }

    private String sanitizeName(String name) {
        return name.trim().toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private String generateTempPassword() {
        StringBuilder sb = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            sb.append(PASSWORD_CHARS.charAt(RANDOM.nextInt(PASSWORD_CHARS.length())));
        }
        return sb.toString();
    }

    private PendingRegistration find(Long id) {
        return registrationRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Registration not found"));
    }

    private String deriveCode(String domain, String orgName) {
        String base = (domain != null && domain.contains(".")) ? domain.substring(0, domain.indexOf('.')) : orgName;
        String code = base.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (code.isEmpty()) code = "UNI";
        String candidate = code; int n = 2;
        while (universityRepository.existsByCode(candidate)) { candidate = code + n; n++; }
        return candidate;
    }

    private RegistrationResponse toResponse(PendingRegistration r) {
        return RegistrationResponse.builder()
                .id(r.getId()).orgName(r.getOrgName()).country(r.getCountry())
                .websiteUrl(r.getWebsiteUrl()).domain(r.getDomain())
                .adminFirstName(r.getAdminFirstName()).adminLastName(r.getAdminLastName())
                .adminContactEmail(r.getAdminContactEmail()).adminPhone(r.getAdminPhone())
                .description(r.getDescription()).status(r.getStatus().name())
                .declineReason(r.getDeclineReason()).submittedAt(r.getSubmittedAt()).reviewedAt(r.getReviewedAt())
                .build();
    }
}
