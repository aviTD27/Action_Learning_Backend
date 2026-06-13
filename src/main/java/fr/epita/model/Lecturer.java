package fr.epita.model;

import fr.epita.enums.LecturerStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

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

    @Column(unique = true)
    private String lecturerRef;

    private String password;

    @ManyToMany
    @JoinTable(
            name = "lecturer_programmes",
            joinColumns = @JoinColumn(name = "lecturer_id"),
            inverseJoinColumns = @JoinColumn(name = "programme_id")
    )
    private List<Programme> programmes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LecturerStatus status = LecturerStatus.ACTIVE;
}
