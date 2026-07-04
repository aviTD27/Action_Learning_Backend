package fr.epita.service;

import fr.epita.dto.Request.CreateSemesterRequest;
import fr.epita.dto.Response.SemesterResponse;
import fr.epita.model.Programme;
import fr.epita.model.Semester;
import fr.epita.repository.CourseRepository;
import fr.epita.repository.ProgrammeRepository;
import fr.epita.repository.SemesterRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SemesterService {

    private final SemesterRepository semesterRepository;
    private final ProgrammeRepository programmeRepository;
    private final CourseRepository courseRepository;

    /** All semesters, optionally scoped to a programme (or a whole university when programmeId is null). */
    public List<SemesterResponse> getAll(Long programmeId, Long universityId) {
        List<Semester> semesters;
        if (programmeId != null) {
            semesters = semesterRepository.findByProgrammeIdOrderByOrderIndexAsc(programmeId);
        } else if (universityId != null) {
            semesters = semesterRepository.findByProgramme_UniversityId(universityId);
        } else {
            semesters = semesterRepository.findAll();
        }
        return semesters.stream().map(this::toResponse).toList();
    }

    @Transactional
    public SemesterResponse create(CreateSemesterRequest request) {
        Programme programme = programmeRepository.findById(request.getProgrammeId())
                .orElseThrow(() -> new EntityNotFoundException("Programme not found"));

        int order = request.getOrderIndex() != null
                ? request.getOrderIndex()
                : semesterRepository.findByProgrammeIdOrderByOrderIndexAsc(programme.getId()).size() + 1;

        Semester semester = Semester.builder()
                .name(request.getName())
                .orderIndex(order)
                .programme(programme)
                .build();
        return toResponse(semesterRepository.save(semester));
    }

    @Transactional
    public SemesterResponse update(Long id, CreateSemesterRequest request) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));
        semester.setName(request.getName());
        if (request.getOrderIndex() != null) semester.setOrderIndex(request.getOrderIndex());
        if (request.getProgrammeId() != null) {
            Programme programme = programmeRepository.findById(request.getProgrammeId())
                    .orElseThrow(() -> new EntityNotFoundException("Programme not found"));
            semester.setProgramme(programme);
        }
        return toResponse(semesterRepository.save(semester));
    }

    @Transactional
    public void delete(Long id) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Semester not found"));
        if (courseRepository.existsBySemesterId(id)) {
            throw new IllegalStateException("Cannot delete a semester that still has courses. Remove them first.");
        }
        semesterRepository.delete(semester);
    }

    private SemesterResponse toResponse(Semester s) {
        return SemesterResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .orderIndex(s.getOrderIndex())
                .programmeId(s.getProgramme() != null ? s.getProgramme().getId() : null)
                .programmeName(s.getProgramme() != null ? s.getProgramme().getName() : null)
                .courseCount(courseRepository.findBySemesterId(s.getId()).size())
                .build();
    }
}
