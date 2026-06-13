package fr.epita.model;

import fr.epita.enums.StudentStatus;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "students")
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password; // TEMPORARY

    @Column(nullable = false, unique = true)
    private String studentRef;

    @ManyToOne
    @JoinColumn(name = "programme_id", nullable = false)
    private Programme programme;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StudentStatus status = StudentStatus.ACTIVE;

    @ManyToOne
    @JoinColumn(name = "cohort_id")
    private Cohort cohort;
}
