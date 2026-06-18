package fr.epita.service;

import fr.epita.dto.Request.CreateRegistrationRequest;
import fr.epita.dto.Response.RegistrationResponse;
import fr.epita.enums.RegistrationStatus;
import fr.epita.model.PendingRegistration;
import fr.epita.model.University;
import fr.epita.repository.PendingRegistrationRepository;
import fr.epita.repository.UniversityRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationService {

    private final PendingRegistrationRepository registrationRepository;
    private final UniversityRepository universityRepository;

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
     * Approve a request: creates the University and marks the request approved.
     * TODO (full flow): atomically create the Uni-Admin user with a generated
     * admin@domain email + temp password, and send the approval email.
     */
    @Transactional
    public RegistrationResponse approve(Long id) {
        PendingRegistration reg = find(id);
        if (reg.getStatus() != RegistrationStatus.PENDING)
            throw new IllegalStateException("Request is not pending");

        String code = deriveCode(reg.getDomain(), reg.getOrgName());
        if (!universityRepository.existsByName(reg.getOrgName()) && !universityRepository.existsByCode(code)) {
            University uni = University.builder().name(reg.getOrgName()).code(code).build();
            universityRepository.save(uni);
        }
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
        return toResponse(registrationRepository.save(reg));
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
