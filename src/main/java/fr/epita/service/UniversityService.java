package fr.epita.service;

import fr.epita.dto.Request.CreateUniversityRequest;
import fr.epita.dto.Response.UniversityResponse;
import fr.epita.model.University;
import fr.epita.repository.UniversityRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UniversityService {

    private final UniversityRepository universityRepository;

    public UniversityResponse create(CreateUniversityRequest request) {
        if (universityRepository.existsByName(request.getName()))
            throw new IllegalStateException("University already exists");
        if (universityRepository.existsByCode(request.getCode()))
            throw new IllegalStateException("University code already exists");

        University university = University.builder()
                .name(request.getName())
                .code(request.getCode())
                .build();

        return toResponse(universityRepository.save(university));
    }

    public List<UniversityResponse> getAll() {
        return universityRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UniversityResponse getById(Long id) {
        University university = universityRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("University not found"));
        return toResponse(university);
    }

    private UniversityResponse toResponse(University university) {
        return UniversityResponse.builder()
                .id(university.getId())
                .name(university.getName())
                .code(university.getCode())
                .domain(university.getDomain())
                .build();
    }
}
