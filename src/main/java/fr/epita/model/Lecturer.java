package fr.epita.model;

import fr.epita.enums.LecturerStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "lecturers")
public class Lecturer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @ManyToOne
    @JoinColumn(name = "programme_id", nullable = false)
    private Programme programme;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LecturerStatus status = LecturerStatus.ACTIVE;
}
