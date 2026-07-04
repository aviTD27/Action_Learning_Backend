package fr.epita.repository;

import fr.epita.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    // Assignments now belong to a Course.
    List<Submission> findByCourseId(Long courseId);

    List<Submission> findByLecturerId(Long lecturerId);

    // All assignments of a programme (course -> programme). Used for the student view.
    List<Submission> findByCourse_Programme_Id(Long programmeId);

    // Analytics: all assignments belonging to a university (course -> programme -> university)
    List<Submission> findByCourse_Programme_University_Id(Long universityId);
}
