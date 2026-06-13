package fr.epita.repository;

import fr.epita.model.Programme;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProgrammeRepository extends JpaRepository<Programme, Long> {

    boolean existsByName(String name);
    List<Programme> findByUniversityId(Long universityId);
}
