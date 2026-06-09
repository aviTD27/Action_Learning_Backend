package fr.epita.service;

import fr.epita.dto.Response.CohortResponse;
import fr.epita.dto.Request.CreateCohortRequest;
import fr.epita.enums.CohortStatus;
import fr.epita.model.Cohort;
import fr.epita.model.Programme;
import fr.epita.repository.CohortRepository;
import fr.epita.repository.ProgrammeRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CohortService {

    private final CohortRepository cohortRepository;
    private final ProgrammeRepository programmeRepository;

    public List<CohortResponse> getAll() {
        return cohortRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CohortResponse create(CreateCohortRequest request) {
        Programme programme = programmeRepository.findById(request.getProgrammeId())
                .orElseThrow(() -> new EntityNotFoundException("Programme not found"));
        Cohort cohort = Cohort.builder()
                .name(request.getName())
                .programme(programme)
                .status(CohortStatus.NOT_STARTED)
                .archived(false)
                .build();

        return toResponse(cohortRepository.save(cohort));
    }

    @Transactional
    public CohortResponse update(Long id, CreateCohortRequest request) {
        Cohort cohort = cohortRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cohort not found"));
        Programme programme = programmeRepository.findById(request.getProgrammeId())
                .orElseThrow(() -> new EntityNotFoundException("Programme not found"));

        cohort.setName(request.getName());
        cohort.setProgramme(programme);

        return toResponse(cohortRepository.save(cohort));
    }

    @Transactional
    public void archive(Long id) {
        Cohort cohort = cohortRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cohort not found"));

        cohort.setArchived(true);
        cohort.setStatus(CohortStatus.ARCHIVED);

        cohortRepository.save(cohort);
    }

    private CohortResponse toResponse(Cohort cohort) {
        return CohortResponse.builder()
                .id(cohort.getId())
                .name(cohort.getName())
                .programmeId(cohort.getProgramme().getId())
                .programmeName(cohort.getProgramme().getName())
                .status(cohort.getStatus().name())
                .build();
    }
}

