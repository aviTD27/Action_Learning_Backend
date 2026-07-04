package fr.epita.repository;

import fr.epita.model.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findBySemesterId(Long semesterId);
    List<Course> findByProgrammeId(Long programmeId);
    List<Course> findByProgramme_UniversityId(Long universityId);
    List<Course> findByLecturerId(Long lecturerId);
    boolean existsBySemesterId(Long semesterId);
}
