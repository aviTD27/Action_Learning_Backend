package fr.epita.service;

import fr.epita.dto.Request.CreateProgrammeRequest;
import fr.epita.dto.Response.ProgrammeResponse;
import fr.epita.model.Programme;
import fr.epita.model.University;
import fr.epita.repository.ProgrammeRepository;
import fr.epita.repository.UniversityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProgrammeService {

    private final ProgrammeRepository programmeRepository;
    private final UniversityRepository universityRepository;

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

    private ProgrammeResponse toResponse(Programme programme) {
        ProgrammeResponse response = new ProgrammeResponse();
        response.setId(programme.getId());
        response.setName(programme.getName());
        response.setCode(programme.getCode());
        response.setDescription(programme.getDescription());
        if (programme.getUniversity() != null) {
            response.setUniversityId(programme.getUniversity().getId());
            response.setUniversityName(programme.getUniversity().getName());
        }
        return response;
    }
}
