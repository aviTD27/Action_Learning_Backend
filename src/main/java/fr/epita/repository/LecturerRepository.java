package fr.epita.repository;

import fr.epita.model.Lecturer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LecturerRepository extends JpaRepository<Lecturer, Long> {


}
