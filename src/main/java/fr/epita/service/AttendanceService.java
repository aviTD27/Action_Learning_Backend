package fr.epita.service;

import fr.epita.dto.Request.CreateAttendanceSessionRequest;
import fr.epita.dto.Request.MarkAttendanceRequest;
import fr.epita.dto.Response.AttendanceRecordResponse;
import fr.epita.dto.Response.AttendanceSessionResponse;
import fr.epita.dto.Response.SessionStudentResponse;
import fr.epita.dto.Response.StudentAttendanceStatsResponse;
import fr.epita.enums.AttendanceStatus;
import fr.epita.enums.Role;
import fr.epita.model.*;
import fr.epita.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final AttendanceSessionRepository sessionRepository;
    private final AttendanceRecordRepository recordRepository;
    private final CohortRepository cohortRepository;
    private final LecturerRepository lecturerRepository;
    private final StudentRepository studentRepository;

    @Transactional
    public AttendanceSessionResponse createSession(CreateAttendanceSessionRequest req, AppUser currentUser) {
        requireLecturer(currentUser);

        Cohort cohort = cohortRepository.findById(req.getCohortId())
                .orElseThrow(() -> new EntityNotFoundException("Cohort not found: " + req.getCohortId()));
        validateCohortUniversity(cohort, currentUser);

        Lecturer lecturer = lecturerRepository.findByEmail(currentUser.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Lecturer profile not found"));

        AttendanceSession session = sessionRepository.save(
                AttendanceSession.builder()
                        .cohort(cohort)
                        .lecturer(lecturer)
                        .sessionDate(req.getSessionDate())
                        .topic(req.getTopic())
                        .build()
        );

        return toSessionResponse(session);
    }

    public List<AttendanceSessionResponse> getSessions(Long cohortId, AppUser currentUser) {
        List<AttendanceSession> sessions;

        if (cohortId != null) {
            Cohort cohort = cohortRepository.findById(cohortId)
                    .orElseThrow(() -> new EntityNotFoundException("Cohort not found"));
            validateCohortUniversity(cohort, currentUser);
            sessions = sessionRepository.findByCohortIdOrderBySessionDateDesc(cohortId);
        } else if (currentUser.getRole() == Role.ROLE_LECTURER) {
            Lecturer lecturer = lecturerRepository.findByEmail(currentUser.getEmail())
                    .orElseThrow(() -> new EntityNotFoundException("Lecturer profile not found"));
            sessions = sessionRepository.findByLecturerIdOrderBySessionDateDesc(lecturer.getId());
        } else {
            Long uniId = currentUser.getUniversityId();
            sessions = uniId != null
                    ? sessionRepository.findByCohort_Programme_UniversityIdOrderBySessionDateDesc(uniId)
                    : sessionRepository.findAll();
        }

        return sessions.stream().map(this::toSessionResponse).toList();
    }

    public List<SessionStudentResponse> getSessionStudents(Long sessionId, AppUser currentUser) {
        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        validateCohortUniversity(session.getCohort(), currentUser);

        List<Student> students = studentRepository.findByCohortId(session.getCohort().getId());
        List<AttendanceRecord> records = recordRepository.findBySessionId(sessionId);

        Map<Long, AttendanceRecord> recordMap = records.stream()
                .collect(Collectors.toMap(r -> r.getStudent().getId(), r -> r));

        return students.stream().map(student -> {
            AttendanceRecord record = recordMap.get(student.getId());
            return SessionStudentResponse.builder()
                    .studentId(student.getId())
                    .firstName(student.getFirstName())
                    .lastName(student.getLastName())
                    .studentRef(student.getStudentRef())
                    .recordId(record != null ? record.getId() : null)
                    .status(record != null ? record.getStatus().name() : null)
                    .minutesLate(record != null ? record.getMinutesLate() : null)
                    .build();
        }).toList();
    }

    @Transactional
    public List<AttendanceRecordResponse> markAttendance(Long sessionId,
                                                         List<MarkAttendanceRequest> requests,
                                                         AppUser currentUser) {
        requireLecturer(currentUser);

        AttendanceSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new EntityNotFoundException("Session not found"));
        validateCohortUniversity(session.getCohort(), currentUser);

        return requests.stream().map(req -> {
            Student student = studentRepository.findById(req.getStudentId())
                    .orElseThrow(() -> new EntityNotFoundException("Student not found: " + req.getStudentId()));

            AttendanceRecord record = recordRepository
                    .findBySessionIdAndStudentId(sessionId, student.getId())
                    .orElseGet(() -> AttendanceRecord.builder()
                            .session(session)
                            .student(student)
                            .build());

            record.setStatus(req.getStatus());
            record.setMinutesLate(req.getStatus() == AttendanceStatus.LATE ? req.getMinutesLate() : null);

            return toRecordResponse(recordRepository.save(record));
        }).toList();
    }

    public List<AttendanceRecordResponse> getMyAttendance(AppUser currentUser) {
        Student student = studentRepository.findByEmail(currentUser.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found"));
        return recordRepository.findByStudentIdOrderBySession_SessionDateDesc(student.getId())
                .stream().map(this::toRecordResponse).toList();
    }

    public StudentAttendanceStatsResponse getMyStats(AppUser currentUser) {
        Student student = studentRepository.findByEmail(currentUser.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found"));
        return buildStats(student);
    }

    public StudentAttendanceStatsResponse getStudentStats(Long studentId, AppUser currentUser) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new EntityNotFoundException("Student not found"));

        if (currentUser.getUniversityId() != null) {
            Long studentUniId = student.getProgramme().getUniversity().getId();
            if (!studentUniId.equals(currentUser.getUniversityId())) {
                throw new AccessDeniedException("Access denied: student belongs to a different university");
            }
        }

        return buildStats(student);
    }

    public List<StudentAttendanceStatsResponse> getCohortStats(Long cohortId, AppUser currentUser) {
        Cohort cohort = cohortRepository.findById(cohortId)
                .orElseThrow(() -> new EntityNotFoundException("Cohort not found"));
        validateCohortUniversity(cohort, currentUser);

        List<Student> students = studentRepository.findByCohortId(cohortId);
        long totalSessions = sessionRepository.countByCohortId(cohortId);

        return students.stream()
                .map(s -> buildStatsForCohort(s, cohort, totalSessions))
                .toList();
    }

    private StudentAttendanceStatsResponse buildStats(Student student) {
        List<StudentAttendanceStatsResponse.CohortStats> cohortStats = List.of();
        if (student.getCohort() != null) {
            long total = sessionRepository.countByCohortId(student.getCohort().getId());
            cohortStats = List.of(buildCohortStats(student, student.getCohort(), total));
        }
        return StudentAttendanceStatsResponse.builder()
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentRef(student.getStudentRef())
                .cohorts(cohortStats)
                .build();
    }

    private StudentAttendanceStatsResponse buildStatsForCohort(Student student, Cohort cohort, long totalSessions) {
        return StudentAttendanceStatsResponse.builder()
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentRef(student.getStudentRef())
                .cohorts(List.of(buildCohortStats(student, cohort, totalSessions)))
                .build();
    }

    private StudentAttendanceStatsResponse.CohortStats buildCohortStats(Student student, Cohort cohort, long totalSessions) {
        List<AttendanceRecord> records = recordRepository.findByStudentIdAndCohortId(student.getId(), cohort.getId());

        int present = (int) records.stream().filter(r -> r.getStatus() == AttendanceStatus.PRESENT).count();
        int late    = (int) records.stream().filter(r -> r.getStatus() == AttendanceStatus.LATE).count();
        int absent  = (int) records.stream().filter(r -> r.getStatus() == AttendanceStatus.ABSENT).count();

        double rate = totalSessions > 0 ? (double) (present + late) / totalSessions * 100 : 0.0;
        double roundedRate = Math.round(rate * 10.0) / 10.0;

        return StudentAttendanceStatsResponse.CohortStats.builder()
                .cohortId(cohort.getId())
                .cohortName(cohort.getName())
                .programmeName(cohort.getProgramme().getName())
                .totalSessions((int) totalSessions)
                .present(present)
                .late(late)
                .absent(absent)
                .attendanceRate(roundedRate)
                .qualifiedForExam(roundedRate >= 80.0)
                .build();
    }

    private AttendanceSessionResponse toSessionResponse(AttendanceSession s) {
        int total  = studentRepository.findByCohortId(s.getCohort().getId()).size();
        int marked = recordRepository.findBySessionId(s.getId()).size();

        return AttendanceSessionResponse.builder()
                .id(s.getId())
                .cohortId(s.getCohort().getId())
                .cohortName(s.getCohort().getName())
                .lecturerId(s.getLecturer().getId())
                .lecturerName(s.getLecturer().getFirstName() + " " + s.getLecturer().getLastName())
                .sessionDate(s.getSessionDate())
                .topic(s.getTopic())
                .createdAt(s.getCreatedAt())
                .totalStudents(total)
                .markedStudents(marked)
                .build();
    }

    private AttendanceRecordResponse toRecordResponse(AttendanceRecord r) {
        AttendanceSession session = r.getSession();
        Student student = r.getStudent();
        return AttendanceRecordResponse.builder()
                .recordId(r.getId())
                .sessionId(session.getId())
                .sessionDate(session.getSessionDate())
                .topic(session.getTopic())
                .cohortName(session.getCohort().getName())
                .studentId(student.getId())
                .studentName(student.getFirstName() + " " + student.getLastName())
                .studentRef(student.getStudentRef())
                .status(r.getStatus().name())
                .minutesLate(r.getMinutesLate())
                .markedAt(r.getMarkedAt())
                .build();
    }

    private void requireLecturer(AppUser user) {
        if (user.getRole() != Role.ROLE_LECTURER) {
            throw new AccessDeniedException("Only lecturers can perform this action");
        }
    }

    private void validateCohortUniversity(Cohort cohort, AppUser currentUser) {
        Long uniId = currentUser.getUniversityId();
        if (uniId == null) return;
        Long cohortUniId = cohort.getProgramme().getUniversity().getId();
        if (!cohortUniId.equals(uniId)) {
            throw new AccessDeniedException("Access denied: cohort belongs to a different university");
        }
    }
}
