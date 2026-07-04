package fr.epita.repository;

import fr.epita.model.Programme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgrammeRepository extends JpaRepository<Programme, Long> {

    boolean existsByNameAndUniversityId(String name, Long universityId);
    List<Programme> findByUniversityId(Long universityId);

    // Programmes attached to a given cohort (intake) via the programme_cohorts join table.
    List<Programme> findByCohorts_Id(Long cohortId);
}
