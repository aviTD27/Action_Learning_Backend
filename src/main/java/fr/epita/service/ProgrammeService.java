package fr.epita.service;

import fr.epita.dto.Request.CreateProgrammeRequest;
import fr.epita.dto.Response.ProgrammeResponse;
import fr.epita.model.Programme;
import fr.epita.model.University;
import fr.epita.repository.CohortRepository;
import fr.epita.repository.LecturerRepository;
import fr.epita.repository.ProgrammeRepository;
import fr.epita.repository.UniversityRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgrammeService {

    private final ProgrammeRepository programmeRepository;
    private final UniversityRepository universityRepository;
    private final CohortRepository cohortRepository;
    private final LecturerRepository lecturerRepository;

    public ProgrammeResponse create(CreateProgrammeRequest request) {

        if (programmeRepository.existsByName(request.getName())) {
            throw new IllegalStateException("Programme already exists");
        }

        University university = universityRepository.findById(request.getUniversityId())
                .orElseThrow(() -> new EntityNotFoundException("University not found"));

        Programme programme = Programme.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .university(university)
                .build();

        return toResponse(programmeRepository.save(programme));
    }

    public List<ProgrammeResponse> getAll(Long universityId) {
        List<Programme> programmes = (universityId != null)
                ? programmeRepository.findByUniversityId(universityId)
                : programmeRepository.findAll();
        return programmes.stream()
                .filter(p -> p.getStatus() == fr.epita.enums.ProgrammeStatus.ACTIVE)  // Exclude archived programmes
                .map(this::toResponse)
                .toList();
    }

    public List<ProgrammeResponse> getArchived(Long universityId) {
        List<Programme> programmes = (universityId != null)
                ? programmeRepository.findByUniversityId(universityId)
                : programmeRepository.findAll();
        return programmes.stream()
                .filter(p -> p.getStatus() == fr.epita.enums.ProgrammeStatus.ARCHIVED)  // Only archived
                .map(this::toResponse)
                .toList();
    }

    public ProgrammeResponse update(Long id, CreateProgrammeRequest request) {
        Programme programme = programmeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Programme not found"));
        University university = universityRepository.findById(request.getUniversityId())
                .orElseThrow(() -> new EntityNotFoundException("University not found"));

        programme.setName(request.getName());
        programme.setCode(request.getCode());
        programme.setDescription(request.getDescription());
        programme.setUniversity(university);

        return toResponse(programmeRepository.save(programme));
    }

    public ProgrammeResponse getById(Long id) {
        Programme programme = programmeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Programme not found"));

        return toResponse(programme);
    }

    /** Archives a programme. Blocked if it has active cohorts or assigned lecturers. */
    @Transactional
    public void archive(Long id) {
        Programme programme = programmeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Programme not found"));

        // Check for active cohorts (NOT_STARTED or ONGOING)
        var activeCohorts = cohortRepository.findByProgramme_Id(id).stream()
                .filter(c -> c.getStatus() == fr.epita.enums.CohortStatus.NOT_STARTED ||
                            c.getStatus() == fr.epita.enums.CohortStatus.ONGOING)
                .toList();

        if (!activeCohorts.isEmpty()) {
            throw new IllegalStateException("Cannot archive a programme with active cohorts. Complete or archive them first.");
        }

        if (!lecturerRepository.findByProgrammes_Id(id).isEmpty()) {
            throw new IllegalStateException("Cannot archive a programme that has lecturers assigned. Unassign them first.");
        }

        programme.setStatus(fr.epita.enums.ProgrammeStatus.ARCHIVED);
        programmeRepository.save(programme);
    }

    /** Restores an archived programme back to active. */
    @Transactional
    public void unarchive(Long id) {
        Programme programme = programmeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Programme not found"));

        if (programme.getStatus() != fr.epita.enums.ProgrammeStatus.ARCHIVED) {
            throw new IllegalStateException("Programme is not archived.");
        }

        programme.setStatus(fr.epita.enums.ProgrammeStatus.ACTIVE);
        programmeRepository.save(programme);
    }

    private ProgrammeResponse toResponse(Programme programme) {
        ProgrammeResponse response = new ProgrammeResponse();
        response.setId(programme.getId());
        response.setName(programme.getName());
        response.setCode(programme.getCode());
        response.setDescription(programme.getDescription());
        response.setStatus(programme.getStatus());
        if (programme.getUniversity() != null) {
            response.setUniversityId(programme.getUniversity().getId());
            response.setUniversityName(programme.getUniversity().getName());
        }
        return response;
    }
}
