package fr.epita.repository;

import fr.epita.model.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CohortRepository extends JpaRepository<Cohort, Long> {
    List<Cohort> findByProgramme_UniversityId(Long universityId);
    boolean existsByProgramme_Id(Long programmeId);
    List<Cohort> findByProgramme_Id(Long programmeId);
}
