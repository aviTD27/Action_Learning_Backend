package fr.epita.repository;

import fr.epita.model.Lecturer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LecturerRepository extends JpaRepository<Lecturer, Long> {
    boolean existsByLecturerRef(String lecturerRef);
    List<Lecturer> findDistinctByProgrammes_UniversityId(Long universityId);
    List<Lecturer> findByProgrammes_Id(Long programmeId);
    Optional<Lecturer> findByEmail(String email);
}
