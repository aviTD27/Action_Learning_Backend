package fr.epita.repository;

import fr.epita.model.Semester;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SemesterRepository extends JpaRepository<Semester, Long> {
    List<Semester> findByProgrammeIdOrderByOrderIndexAsc(Long programmeId);
    List<Semester> findByProgramme_UniversityId(Long universityId);
    boolean existsByProgrammeId(Long programmeId);
}
