package fr.epita.service;

import fr.epita.dto.Request.CreateCohortRequest;
import fr.epita.dto.Response.CohortResponse;
import fr.epita.enums.CohortSeason;
import fr.epita.enums.CohortStatus;
import fr.epita.model.Cohort;
import fr.epita.model.Programme;
import fr.epita.model.University;
import fr.epita.repository.CohortRepository;
import fr.epita.repository.ProgrammeRepository;
import fr.epita.repository.StudentRepository;
import fr.epita.repository.UniversityRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * A Cohort is an INTAKE SEASON (e.g. "Spring 2026"), university-wide. Programmes are attached
 * to it via a many-to-many relationship (owned by Programme.cohorts).
 */
@Service
@RequiredArgsConstructor
public class CohortService {

    private final CohortRepository cohortRepository;
    private final ProgrammeRepository programmeRepository;
    private final UniversityRepository universityRepository;
    private final StudentRepository studentRepository;

    public List<CohortResponse> getAll(Long universityId) {
        List<Cohort> cohorts = (universityId != null)
                ? cohortRepository.findByUniversityId(universityId)
                : cohortRepository.findAll();
        return cohorts.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CohortResponse create(CreateCohortRequest request, Long universityId) {
        if (universityId == null) {
            throw new IllegalStateException("A university context is required to create a cohort.");
        }
        University university = universityRepository.findById(universityId)
                .orElseThrow(() -> new EntityNotFoundException("University not found"));

        Cohort cohort = Cohort.builder()
                .name(resolveName(request))
                .season(request.getSeason())
                .academicYear(request.getAcademicYear())
                .university(university)
                .status(request.getStatus() != null ? request.getStatus() : CohortStatus.NOT_STARTED)
                .build();

        Cohort saved = cohortRepository.save(cohort);
        setProgrammes(saved, request.getProgrammeIds());
        return toResponse(saved);
    }

    @Transactional
    public CohortResponse update(Long id, CreateCohortRequest request) {
        Cohort cohort = cohortRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cohort not found"));

        cohort.setName(resolveName(request));
        if (request.getSeason() != null) cohort.setSeason(request.getSeason());
        if (request.getAcademicYear() != null) cohort.setAcademicYear(request.getAcademicYear());
        if (request.getStatus() != null) cohort.setStatus(request.getStatus());
        Cohort saved = cohortRepository.save(cohort);

        if (request.getProgrammeIds() != null) {
            setProgrammes(saved, request.getProgrammeIds());
        }
        return toResponse(saved);
    }

    /** Derives "Spring 2026" from season + year when no explicit name is provided. */
    private String resolveName(CreateCohortRequest request) {
        if (request.getName() != null && !request.getName().isBlank()) {
            return request.getName().trim();
        }
        String seasonLabel = request.getSeason() == CohortSeason.SPRING ? "Spring" : "Fall";
        return seasonLabel + " " + request.getAcademicYear();
    }

    /**
     * Reconciles the programmes attached to this cohort. The join table is owned by
     * Programme.cohorts, so we mutate from the programme side.
     */
    private void setProgrammes(Cohort cohort, List<Long> programmeIds) {
        List<Long> desiredIds = programmeIds != null ? programmeIds : new ArrayList<>();
        List<Programme> current = programmeRepository.findByCohorts_Id(cohort.getId());

        // Detach programmes no longer selected.
        for (Programme p : current) {
            if (!desiredIds.contains(p.getId())) {
                p.getCohorts().removeIf(c -> c.getId().equals(cohort.getId()));
                programmeRepository.save(p);
            }
        }
        // Attach newly selected programmes.
        for (Long pid : desiredIds) {
            Programme p = programmeRepository.findById(pid)
                    .orElseThrow(() -> new EntityNotFoundException("Programme not found: " + pid));
            if (p.getCohorts() == null) p.setCohorts(new ArrayList<>());
            boolean already = p.getCohorts().stream().anyMatch(c -> c.getId().equals(cohort.getId()));
            if (!already) {
                p.getCohorts().add(cohort);
                programmeRepository.save(p);
            }
        }
    }

    private CohortResponse toResponse(Cohort cohort) {
        List<Programme> programmes = programmeRepository.findByCohorts_Id(cohort.getId());
        int studentCount = studentRepository.findByCohortId(cohort.getId()).size();
        return CohortResponse.builder()
                .id(cohort.getId())
                .name(cohort.getName())
                .season(cohort.getSeason() != null ? cohort.getSeason().name() : null)
                .academicYear(cohort.getAcademicYear())
                .universityId(cohort.getUniversity() != null ? cohort.getUniversity().getId() : null)
                .universityName(cohort.getUniversity() != null ? cohort.getUniversity().getName() : null)
                .status(cohort.getStatus().name())
                .programmeIds(programmes.stream().map(Programme::getId).toList())
                .programmeNames(programmes.stream().map(Programme::getName).toList())
                .studentCount(studentCount)
                .build();
    }
}
