package fr.epita.service;

import fr.epita.dto.Request.CreateLecturerRequest;
import fr.epita.dto.Response.LecturerResponse;
import fr.epita.enums.LecturerStatus;
import fr.epita.model.Lecturer;
import fr.epita.model.Programme;
import fr.epita.repository.LecturerRepository;
import fr.epita.repository.ProgrammeRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LecturerService {

    private final LecturerRepository lecturerRepository;
    private final ProgrammeRepository programmeRepository;

    public List<LecturerResponse> getAll(Long universityId) {
        List<Lecturer> lecturers = (universityId != null)
                ? lecturerRepository.findDistinctByProgrammes_UniversityId(universityId)
                : lecturerRepository.findAll();
        return lecturers.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LecturerResponse create(CreateLecturerRequest request) {
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new IllegalStateException("Password is required");
        }
        if (lecturerRepository.existsByLecturerRef(request.getLecturerRef())) {
            throw new IllegalStateException("Lecturer reference already exists");
        }
        List<Programme> programmes = resolveProgrammes(request.getProgrammeIds());

        Lecturer lecturer = Lecturer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .lecturerRef(request.getLecturerRef())
                .password(request.getPassword())
                .programmes(programmes)
                .status(LecturerStatus.ACTIVE)
                .build();

        return toResponse(lecturerRepository.save(lecturer));
    }

    @Transactional
    public LecturerResponse update(Long id, CreateLecturerRequest request) {
        Lecturer lecturer = lecturerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lecturer not found"));

        List<Programme> programmes = resolveProgrammes(request.getProgrammeIds());

        lecturer.setFirstName(request.getFirstName());
        lecturer.setLastName(request.getLastName());
        lecturer.setEmail(request.getEmail());
        lecturer.setLecturerRef(request.getLecturerRef());
        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            lecturer.setPassword(request.getPassword());
        }
        lecturer.setProgrammes(programmes);
        if (request.getStatus() != null) {
            lecturer.setStatus(request.getStatus());
        }

        return toResponse(lecturerRepository.save(lecturer));
    }

    private List<Programme> resolveProgrammes(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new EntityNotFoundException("At least one programme is required");
        }
        List<Programme> programmes = programmeRepository.findAllById(ids);
        if (programmes.size() != ids.size()) {
            throw new EntityNotFoundException("Programme not found");
        }
        return programmes;
    }

    private LecturerResponse toResponse(Lecturer lecturer) {
        return LecturerResponse.builder()
                .id(lecturer.getId())
                .firstName(lecturer.getFirstName())
                .lastName(lecturer.getLastName())
                .email(lecturer.getEmail())
                .lecturerRef(lecturer.getLecturerRef())
                .programmeIds(lecturer.getProgrammes().stream().map(Programme::getId).toList())
                .programmeNames(lecturer.getProgrammes().stream().map(Programme::getName).toList())
                .status(lecturer.getStatus().name())
                .build();
    }
}
