package fr.epita.repository;

import fr.epita.model.Lecturer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LecturerRepository extends JpaRepository<Lecturer, Long> {
    boolean existsByLecturerRef(String lecturerRef);
    List<Lecturer> findDistinctByProgrammes_UniversityId(Long universityId);
}
