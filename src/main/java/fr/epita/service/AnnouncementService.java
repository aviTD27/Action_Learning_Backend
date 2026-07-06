package fr.epita.service;

import fr.epita.dto.Request.SendAnnouncementRequest;
import fr.epita.dto.Response.AnnouncementResponse;
import fr.epita.dto.Response.SentAnnouncementResponse;
import fr.epita.enums.AnnouncementAudience;
import fr.epita.enums.Role;
import fr.epita.model.*;
import fr.epita.repository.*;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AnnouncementStudentRecipientRepository studentRecipientRepository;
    private final AnnouncementLecturerRecipientRepository lecturerRecipientRepository;
    private final StudentRepository studentRepository;
    private final LecturerRepository lecturerRepository;
    private final CohortRepository cohortRepository;

    @Transactional
    public void send(SendAnnouncementRequest request, AppUser sender) {
        Role role = sender.getRole();
        if (role != Role.ROLE_UNI_ADMIN && role != Role.ROLE_LECTURER) {
            throw new AccessDeniedException("Only lecturers and university admins can send announcements");
        }

        AnnouncementAudience audience = request.getAudience();

        // Lecturers may only target students, not other lecturers
        if (role == Role.ROLE_LECTURER &&
                (audience == AnnouncementAudience.ALL_UNIVERSITY_LECTURERS
                        || audience == AnnouncementAudience.SPECIFIC_LECTURERS)) {
            throw new AccessDeniedException("Lecturers can only send announcements to students");
        }

        String senderName = sender.getFirstName() + " " + sender.getSurname();
        Long universityId = sender.getUniversityId();

        Announcement announcement = announcementRepository.save(
                Announcement.builder()
                        .subject(request.getSubject())
                        .message(request.getMessage())
                        .senderName(senderName)
                        .senderRole(role)
                        .universityId(universityId)
                        .senderEmail(sender.getEmail())
                        .cohortId(request.getCohortId())
                        .audience(audience)
                        .build()
        );

        switch (audience) {
            case ALL_COHORT_STUDENTS -> {
                if (request.getCohortId() == null)
                    throw new IllegalStateException("cohortId is required for ALL_COHORT_STUDENTS");
                List<Student> students = studentRepository.findByCohortId(request.getCohortId());
                students.forEach(s -> createStudentRecipient(announcement, s));
            }
            case SPECIFIC_STUDENTS -> {
                if (request.getStudentIds() == null || request.getStudentIds().isEmpty())
                    throw new IllegalStateException("studentIds is required for SPECIFIC_STUDENTS");
                request.getStudentIds().forEach(id -> {
                    Student student = studentRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Student not found: " + id));
                    createStudentRecipient(announcement, student);
                });
            }
            case ALL_UNIVERSITY_STUDENTS -> {
                List<Student> students = studentRepository.findByProgramme_UniversityId(universityId);
                students.forEach(s -> createStudentRecipient(announcement, s));
            }
            case ALL_UNIVERSITY_LECTURERS -> {
                List<Lecturer> lecturers = lecturerRepository.findDistinctByProgrammes_UniversityId(universityId);
                lecturers.forEach(l -> createLecturerRecipient(announcement, l));
            }
            case SPECIFIC_LECTURERS -> {
                if (request.getLecturerIds() == null || request.getLecturerIds().isEmpty())
                    throw new IllegalStateException("lecturerIds is required for SPECIFIC_LECTURERS");
                request.getLecturerIds().forEach(id -> {
                    Lecturer lecturer = lecturerRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Lecturer not found: " + id));
                    createLecturerRecipient(announcement, lecturer);
                });
            }
        }
    }

    /** Student inbox */
    public List<AnnouncementResponse> getInboxForStudent(AppUser currentUser) {
        Student student = studentRepository.findByEmail(currentUser.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found"));
        return studentRecipientRepository
                .findByStudentIdOrderByAnnouncement_SentAtDesc(student.getId())
                .stream()
                .map(this::toStudentResponse)
                .toList();
    }

    public long unreadCountForStudent(AppUser currentUser) {
        Student student = studentRepository.findByEmail(currentUser.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found"));
        return studentRecipientRepository.findByStudentIdAndReadFlagFalse(student.getId()).size();
    }

    /** Lecturer inbox */
    public List<AnnouncementResponse> getInboxForLecturer(AppUser currentUser) {
        Lecturer lecturer = lecturerRepository.findByEmail(currentUser.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Lecturer profile not found"));
        return lecturerRecipientRepository
                .findByLecturerIdOrderByAnnouncement_SentAtDesc(lecturer.getId())
                .stream()
                .map(this::toLecturerResponse)
                .toList();
    }

    public long unreadCountForLecturer(AppUser currentUser) {
        Lecturer lecturer = lecturerRepository.findByEmail(currentUser.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Lecturer profile not found"));
        return lecturerRecipientRepository.findByLecturerIdAndReadFlagFalse(lecturer.getId()).size();
    }

    /** Mark a student recipient row as read */
    @Transactional
    public void markStudentRead(Long recipientId) {
        AnnouncementStudentRecipient row = studentRecipientRepository.findById(recipientId)
                .orElseThrow(() -> new EntityNotFoundException("Recipient entry not found"));
        row.setReadFlag(true);
        studentRecipientRepository.save(row);
    }

    /** Mark all student announcement rows as read */
    @Transactional
    public void markAllStudentRead(AppUser currentUser) {
        Student student = studentRepository.findByEmail(currentUser.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Student profile not found"));
        List<AnnouncementStudentRecipient> unread =
                studentRecipientRepository.findByStudentIdAndReadFlagFalse(student.getId());
        unread.forEach(r -> r.setReadFlag(true));
        studentRecipientRepository.saveAll(unread);
    }

    /** Mark a lecturer recipient row as read */
    @Transactional
    public void markLecturerRead(Long recipientId) {
        AnnouncementLecturerRecipient row = lecturerRecipientRepository.findById(recipientId)
                .orElseThrow(() -> new EntityNotFoundException("Recipient entry not found"));
        row.setReadFlag(true);
        lecturerRecipientRepository.save(row);
    }

    /** Mark all lecturer announcement rows as read */
    @Transactional
    public void markAllLecturerRead(AppUser currentUser) {
        Lecturer lecturer = lecturerRepository.findByEmail(currentUser.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("Lecturer profile not found"));
        List<AnnouncementLecturerRecipient> unread =
                lecturerRecipientRepository.findByLecturerIdAndReadFlagFalse(lecturer.getId());
        unread.forEach(r -> r.setReadFlag(true));
        lecturerRecipientRepository.saveAll(unread);
    }

    /** Sent-outbox for the current user (UNI_ADMIN or Lecturer) */
    public List<SentAnnouncementResponse> getSent(AppUser currentUser) {
        return announcementRepository
                .findBySenderEmailOrderBySentAtDesc(currentUser.getEmail())
                .stream()
                .map(this::toSentResponse)
                .toList();
    }

    private SentAnnouncementResponse toSentResponse(Announcement a) {
        long studentCount  = studentRecipientRepository.countByAnnouncementId(a.getId());
        long lecturerCount = lecturerRecipientRepository.countByAnnouncementId(a.getId());

        String cohortName = null;
        if (a.getAudience() == AnnouncementAudience.ALL_COHORT_STUDENTS && a.getCohortId() != null) {
            cohortName = cohortRepository.findById(a.getCohortId())
                    .map(c -> c.getName())
                    .orElse(null);
        }

        return SentAnnouncementResponse.builder()
                .id(a.getId())
                .subject(a.getSubject())
                .message(a.getMessage())
                .audience(a.getAudience().name())
                .cohortName(cohortName)
                .recipientCount((int) (studentCount + lecturerCount))
                .sentAt(a.getSentAt())
                .build();
    }

    private void createStudentRecipient(Announcement announcement, Student student) {
        studentRecipientRepository.save(
                AnnouncementStudentRecipient.builder()
                        .announcement(announcement)
                        .student(student)
                        .build()
        );
    }

    private void createLecturerRecipient(Announcement announcement, Lecturer lecturer) {
        lecturerRecipientRepository.save(
                AnnouncementLecturerRecipient.builder()
                        .announcement(announcement)
                        .lecturer(lecturer)
                        .build()
        );
    }

    private AnnouncementResponse toStudentResponse(AnnouncementStudentRecipient r) {
        return toResponse(r.getId(), r.getAnnouncement(), r.isReadFlag());
    }

    private AnnouncementResponse toLecturerResponse(AnnouncementLecturerRecipient r) {
        return toResponse(r.getId(), r.getAnnouncement(), r.isReadFlag());
    }

    private AnnouncementResponse toResponse(Long recipientId, Announcement a, boolean read) {
        return AnnouncementResponse.builder()
                .recipientId(recipientId)
                .announcementId(a.getId())
                .subject(a.getSubject())
                .message(a.getMessage())
                .senderName(a.getSenderName())
                .senderRole(a.getSenderRole().name())
                .audience(a.getAudience().name())
                .sentAt(a.getSentAt())
                .read(read)
                .build();
    }
}
