package fr.epita.service;

import fr.epita.dto.Request.CreateProgrammeRequest;
import fr.epita.dto.Response.ProgrammeResponse;
import fr.epita.enums.ProgrammeStatus;
import fr.epita.model.Cohort;
import fr.epita.model.Programme;
import fr.epita.model.University;
import fr.epita.repository.CohortRepository;
import fr.epita.repository.LecturerRepository;
import fr.epita.repository.ProgrammeRepository;
import fr.epita.repository.SemesterRepository;
import fr.epita.repository.StudentRepository;
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
    private final SemesterRepository semesterRepository;
    private final StudentRepository studentRepository;

    public ProgrammeResponse create(CreateProgrammeRequest request) {

        if (programmeRepository.existsByNameAndUniversityId(request.getName(), request.getUniversityId())) {
            throw new IllegalStateException("A programme with this name already exists for this university");
        }

        University university = universityRepository.findById(request.getUniversityId())
                .orElseThrow(() -> new EntityNotFoundException("University not found"));

        Programme programme = Programme.builder()
                .name(request.getName())
                .code(request.getCode())
                .description(request.getDescription())
                .university(university)
                .status(ProgrammeStatus.ACTIVE)
                .build();

        return toResponse(programmeRepository.save(programme));
    }

    public List<ProgrammeResponse> getAll(Long universityId) {
        List<Programme> programmes = (universityId != null)
                ? programmeRepository.findByUniversityId(universityId)
                : programmeRepository.findAll();
        return programmes.stream()
                .filter(p -> p.getStatus() == ProgrammeStatus.ACTIVE)
                .map(this::toResponse)
                .toList();
    }

    public List<ProgrammeResponse> getArchived(Long universityId) {
        List<Programme> programmes = (universityId != null)
                ? programmeRepository.findByUniversityId(universityId)
                : programmeRepository.findAll();
        return programmes.stream()
                .filter(p -> p.getStatus() == ProgrammeStatus.ARCHIVED)
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

    /** Archives a programme. Blocked if it still has enrolled students or assigned lecturers. */
    @Transactional
    public void archive(Long id) {
        Programme programme = programmeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Programme not found"));

        if (!studentRepository.findByProgrammeId(id).isEmpty()) {
            throw new IllegalStateException("Cannot archive a programme that has enrolled students. Move or remove them first.");
        }
        if (!lecturerRepository.findByProgrammes_Id(id).isEmpty()) {
            throw new IllegalStateException("Cannot archive a programme that has lecturers assigned. Unassign them first.");
        }

        programme.setStatus(ProgrammeStatus.ARCHIVED);
        programmeRepository.save(programme);
    }

    /** Restores an archived programme back to active. */
    @Transactional
    public void unarchive(Long id) {
        Programme programme = programmeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Programme not found"));

        if (programme.getStatus() != ProgrammeStatus.ARCHIVED) {
            throw new IllegalStateException("Programme is not archived.");
        }
        programme.setStatus(ProgrammeStatus.ACTIVE);
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
        List<Cohort> cohorts = cohortRepository.findByProgrammes_Id(programme.getId());
        response.setCohortIds(cohorts.stream().map(Cohort::getId).toList());
        response.setCohortNames(cohorts.stream().map(Cohort::getName).toList());
        response.setSemesterCount(semesterRepository.findByProgrammeIdOrderByOrderIndexAsc(programme.getId()).size());
        return response;
    }
}
