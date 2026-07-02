package fr.epita.repository;

import fr.epita.model.TimetableEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimetableEntryRepository extends JpaRepository<TimetableEntry, Long> {

    // Admin: all entries for a university (via cohort → programme → university)
    List<TimetableEntry> findByCohort_Programme_UniversityId(Long universityId);

    // Student: entries for their specific cohort
    List<TimetableEntry> findByCohortId(Long cohortId);

    // Lecturer: entries they teach
    List<TimetableEntry> findByLecturerId(Long lecturerId);
}
