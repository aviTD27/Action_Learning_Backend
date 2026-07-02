package fr.epita.service;

import fr.epita.dto.Response.CohortResponse;
import fr.epita.dto.Request.CreateCohortRequest;
import fr.epita.enums.CohortStatus;
import fr.epita.model.Cohort;
import fr.epita.model.Lecturer;
import fr.epita.model.Programme;
import fr.epita.repository.CohortRepository;
import fr.epita.repository.LecturerRepository;
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
    private final LecturerRepository lecturerRepository;

    public List<CohortResponse> getAll(Long universityId) {
        List<Cohort> cohorts = (universityId != null)
                ? cohortRepository.findByProgramme_UniversityId(universityId)
                : cohortRepository.findAll();
        return cohorts.stream()
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
                .lecturers(resolveLecturers(request.getLecturerIds()))
                .build();

        return toResponse(cohortRepository.save(cohort));
    }

    @Transactional
    public CohortResponse update(Long id, CreateCohortRequest request) {
        Cohort cohort = cohortRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cohort not found"));

        // A cohort under an archived programme is read-only: block status/other changes.
        if (cohort.getProgramme() != null
                && cohort.getProgramme().getStatus() == fr.epita.enums.ProgrammeStatus.ARCHIVED) {
            throw new IllegalStateException("Cannot modify a cohort whose programme is archived. Restore the programme first.");
        }

        Programme programme = programmeRepository.findById(request.getProgrammeId())
                .orElseThrow(() -> new EntityNotFoundException("Programme not found"));

        cohort.setName(request.getName());
        cohort.setProgramme(programme);
        if (request.getStatus() != null) {
            cohort.setStatus(request.getStatus());
        }
        if (request.getLecturerIds() != null) {
            cohort.setLecturers(resolveLecturers(request.getLecturerIds()));
        }

        return toResponse(cohortRepository.save(cohort));
    }

    /** Loads the lecturers for the given ids; returns an empty list when none are supplied. */
    private List<Lecturer> resolveLecturers(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Lecturer> lecturers = lecturerRepository.findAllById(ids);
        if (lecturers.size() != ids.size()) {
            throw new EntityNotFoundException("One or more lecturers not found");
        }
        return lecturers;
    }

    private CohortResponse toResponse(Cohort cohort) {
        List<Lecturer> lecturers = cohort.getLecturers() != null ? cohort.getLecturers() : List.of();
        return CohortResponse.builder()
                .id(cohort.getId())
                .name(cohort.getName())
                .programmeId(cohort.getProgramme().getId())
                .programmeName(cohort.getProgramme().getName())
                .status(cohort.getStatus().name())
                .lecturerIds(lecturers.stream().map(Lecturer::getId).toList())
                .lecturerNames(lecturers.stream()
                        .map(l -> l.getFirstName() + " " + l.getLastName())
                        .toList())
                .build();
    }
}

