package fr.epita.repository;

import fr.epita.model.AttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceSessionRepository extends JpaRepository<AttendanceSession, Long> {
    List<AttendanceSession> findByCohortIdOrderBySessionDateDesc(Long cohortId);
    List<AttendanceSession> findByLecturerIdOrderBySessionDateDesc(Long lecturerId);
    List<AttendanceSession> findByCohort_UniversityIdOrderBySessionDateDesc(Long universityId);
    long countByCohortId(Long cohortId);
}
