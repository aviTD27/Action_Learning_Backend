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

    public List<LecturerResponse> getAll() {
        return lecturerRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LecturerResponse create(CreateLecturerRequest request) {
        Programme programme = programmeRepository.findById(request.getProgrammeId())
                .orElseThrow(() -> new EntityNotFoundException("Programme not found"));

        Lecturer lecturer = Lecturer.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .programme(programme)
                .status(LecturerStatus.ACTIVE)
                .build();

        return toResponse(lecturerRepository.save(lecturer));
    }

    @Transactional
    public LecturerResponse update(Long id, CreateLecturerRequest request) {
        Lecturer lecturer = lecturerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lecturer not found"));

        Programme programme = programmeRepository.findById(request.getProgrammeId())
                .orElseThrow(() -> new EntityNotFoundException("Programme not found"));

        lecturer.setFirstName(request.getFirstName());
        lecturer.setLastName(request.getLastName());
        lecturer.setEmail(request.getEmail());
        lecturer.setProgramme(programme);

        return toResponse(lecturerRepository.save(lecturer));
    }

    @Transactional
    public void archive(Long id) {
        Lecturer lecturer = lecturerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Lecturer not found"));

        lecturer.setStatus(LecturerStatus.INACTIVE);

        lecturerRepository.save(lecturer);
    }

    private LecturerResponse toResponse(Lecturer lecturer) {
        return LecturerResponse.builder()
                .id(lecturer.getId())
                .firstName(lecturer.getFirstName())
                .lastName(lecturer.getLastName())
                .email(lecturer.getEmail())
                .programmeId(lecturer.getProgramme().getId())
                .programmeName(lecturer.getProgramme().getName())
                .status(lecturer.getStatus().name())
                .build();
    }
}
