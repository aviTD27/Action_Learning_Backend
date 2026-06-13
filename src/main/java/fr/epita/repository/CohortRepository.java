package fr.epita.repository;

import fr.epita.model.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CohortRepository extends JpaRepository<Cohort, Long> {
    List<Cohort> findByProgramme_UniversityId(Long universityId);
}
