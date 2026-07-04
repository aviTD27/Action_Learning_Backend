package fr.epita.repository;

import fr.epita.enums.AttendanceStatus;
import fr.epita.model.AttendanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    List<AttendanceRecord> findBySessionId(Long sessionId);
    Optional<AttendanceRecord> findBySessionIdAndStudentId(Long sessionId, Long studentId);
    List<AttendanceRecord> findByStudentIdOrderBySession_SessionDateDesc(Long studentId);

    long countBySessionIdAndStatus(Long sessionId, AttendanceStatus status);

    // For student/admin stats: all records for a student within a specific cohort's sessions
    @Query("SELECT r FROM AttendanceRecord r " +
           "WHERE r.student.id = :studentId " +
           "AND r.session.cohort.id = :cohortId")
    List<AttendanceRecord> findByStudentIdAndCohortId(
            @Param("studentId") Long studentId,
            @Param("cohortId") Long cohortId);

    // Count total sessions that have at least one record (used for stats denominator)
    @Query("SELECT COUNT(DISTINCT r.session.id) FROM AttendanceRecord r " +
           "WHERE r.student.id = :studentId AND r.session.cohort.id = :cohortId")
    long countDistinctSessionsByStudentAndCohort(
            @Param("studentId") Long studentId,
            @Param("cohortId") Long cohortId);
}
