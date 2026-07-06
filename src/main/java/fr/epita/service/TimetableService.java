package fr.epita.service;

import fr.epita.dto.Request.CreateTimetableRequest;
import fr.epita.dto.Response.TimetableResponse;
import fr.epita.enums.Role;
import fr.epita.model.AppUser;
import fr.epita.model.Cohort;
import fr.epita.model.Lecturer;
import fr.epita.model.Student;
import fr.epita.model.TimetableEntry;
import fr.epita.repository.CohortRepository;
import fr.epita.repository.LecturerRepository;
import fr.epita.repository.StudentRepository;
import fr.epita.repository.TimetableEntryRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableEntryRepository timetableRepository;
    private final CohortRepository cohortRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;

    public List<TimetableResponse> getAll(AppUser currentUser) {
        List<TimetableEntry> entries;

        Role role = currentUser.getRole();

        if (role == Role.ROLE_SUPER_ADMIN || role == Role.ROLE_PLATFORM_ADMIN) {
            entries = timetableRepository.findAll();

        } else if (role == Role.ROLE_UNI_ADMIN) {
            entries = timetableRepository.findByCohort_UniversityId(currentUser.getUniversityId());

        } else if (role == Role.ROLE_STUDENT) {
            Student student = studentRepository.findByEmail(currentUser.getEmail())
                    .orElseThrow(() -> new EntityNotFoundException("Student profile not found"));
            if (student.getCohort() == null) return List.of();
            entries = timetableRepository.findByCohortId(student.getCohort().getId());

        } else if (role == Role.ROLE_LECTURER) {
            Lecturer lecturer = lecturerRepository.findByEmail(currentUser.getEmail())
                    .orElseThrow(() -> new EntityNotFoundException("Lecturer profile not found"));
            entries = timetableRepository.findByLecturerId(lecturer.getId());

        } else {
            return List.of();
        }

        return entries.stream().map(this::toResponse).toList();
    }

    /**
     * Returns all timetable entries grouped by day of week, sorted by startTime within each day.
     * Days with no entries are omitted. Multiple entries at the same time are preserved as a list.
     */
    public Map<String, List<TimetableResponse>> getWeekly(AppUser currentUser) {
        List<TimetableResponse> all = getAll(currentUser);

        // Maintain calendar order: Mon → Sun
        Map<String, List<TimetableResponse>> grouped = new LinkedHashMap<>();
        Arrays.stream(DayOfWeek.values()).forEach(day ->
                grouped.put(day.name(), new java.util.ArrayList<>()));

        all.forEach(entry -> grouped.get(entry.getDayOfWeek().name()).add(entry));

        // Sort each day's list by startTime, then remove empty days
        grouped.values().forEach(list ->
                list.sort(java.util.Comparator.comparing(TimetableResponse::getStartTime)));
        grouped.entrySet().removeIf(e -> e.getValue().isEmpty());

        return grouped;
    }

    @Transactional
    public TimetableResponse create(CreateTimetableRequest request, AppUser currentUser) {
        requireUniAdmin(currentUser);

        Cohort cohort = cohortRepository.findById(request.getCohortId())
                .orElseThrow(() -> new EntityNotFoundException("Cohort not found"));

        validateUniversityAccess(cohort.getUniversity().getId(), currentUser);

        Lecturer lecturer = null;
        if (request.getLecturerId() != null) {
            lecturer = lecturerRepository.findById(request.getLecturerId())
                    .orElseThrow(() -> new EntityNotFoundException("Lecturer not found"));
        }

        TimetableEntry entry = TimetableEntry.builder()
                .title(request.getTitle())
                .room(request.getRoom())
                .dayOfWeek(request.getDayOfWeek())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .color(request.getColor())
                .cohort(cohort)
                .lecturer(lecturer)
                .build();

        return toResponse(timetableRepository.save(entry));
    }

    @Transactional
    public TimetableResponse update(Long id, CreateTimetableRequest request, AppUser currentUser) {
        requireUniAdmin(currentUser);

        TimetableEntry entry = find(id);
        validateUniversityAccess(entry.getCohort().getUniversity().getId(), currentUser);

        Cohort cohort = cohortRepository.findById(request.getCohortId())
                .orElseThrow(() -> new EntityNotFoundException("Cohort not found"));
        validateUniversityAccess(cohort.getUniversity().getId(), currentUser);

        Lecturer lecturer = null;
        if (request.getLecturerId() != null) {
            lecturer = lecturerRepository.findById(request.getLecturerId())
                    .orElseThrow(() -> new EntityNotFoundException("Lecturer not found"));
        }

        entry.setTitle(request.getTitle());
        entry.setRoom(request.getRoom());
        entry.setDayOfWeek(request.getDayOfWeek());
        entry.setStartTime(request.getStartTime());
        entry.setEndTime(request.getEndTime());
        entry.setColor(request.getColor());
        entry.setCohort(cohort);
        entry.setLecturer(lecturer);

        return toResponse(timetableRepository.save(entry));
    }

    @Transactional
    public void delete(Long id, AppUser currentUser) {
        requireUniAdmin(currentUser);
        TimetableEntry entry = find(id);
        validateUniversityAccess(entry.getCohort().getUniversity().getId(), currentUser);
        timetableRepository.delete(entry);
    }

    private TimetableEntry find(Long id) {
        return timetableRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Timetable entry not found"));
    }

    private void requireUniAdmin(AppUser currentUser) {
        if (currentUser.getRole() != Role.ROLE_UNI_ADMIN
                && currentUser.getRole() != Role.ROLE_SUPER_ADMIN
                && currentUser.getRole() != Role.ROLE_PLATFORM_ADMIN) {
            throw new AccessDeniedException("Only university admins can manage the timetable");
        }
    }

    private void validateUniversityAccess(Long resourceUniversityId, AppUser currentUser) {
        if (currentUser.getUniversityId() == null) return;
        if (!currentUser.getUniversityId().equals(resourceUniversityId)) {
            throw new AccessDeniedException("Access denied: resource belongs to a different university");
        }
    }

    private TimetableResponse toResponse(TimetableEntry e) {
        List<String> programmeNames = e.getCohort().getProgrammes() != null
                ? e.getCohort().getProgrammes().stream()
                        .map(p -> p.getName())
                        .collect(Collectors.toList())
                : List.of();

        return TimetableResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .room(e.getRoom())
                .dayOfWeek(e.getDayOfWeek())
                .startTime(e.getStartTime())
                .endTime(e.getEndTime())
                .color(e.getColor())
                .cohortId(e.getCohort().getId())
                .cohortName(e.getCohort().getName())
                .programmeNames(programmeNames)
                .lecturerId(e.getLecturer() != null ? e.getLecturer().getId() : null)
                .lecturerName(e.getLecturer() != null
                        ? e.getLecturer().getFirstName() + " " + e.getLecturer().getLastName()
                        : null)
                .universityId(e.getCohort().getUniversity() != null
                        ? e.getCohort().getUniversity().getId()
                        : null)
                .universityName(e.getCohort().getUniversity() != null
                        ? e.getCohort().getUniversity().getName()
                        : null)
                .build();
    }
}
