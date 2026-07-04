package fr.epita.repository;

import fr.epita.model.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CohortRepository extends JpaRepository<Cohort, Long> {

    // Cohorts (intakes) are now scoped directly to a university.
    List<Cohort> findByUniversityId(Long universityId);

    // Cohorts (intakes) a given programme runs in (via the programme_cohorts M2M).
    List<Cohort> findByProgrammes_Id(Long programmeId);

    boolean existsByProgrammes_Id(Long programmeId);
}
