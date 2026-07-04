package fr.epita.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.epita.enums.ProgrammeStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "programmes")
public class Programme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private String description;

    @ManyToMany(mappedBy = "programmes")
    @JsonIgnore
    private List<Lecturer> lecturers;

    @ManyToOne
    @JoinColumn(name = "university_id")
    private University university;

    /** Intakes (cohorts) in which this programme runs. Owning side of the M2M. */
    @ManyToMany
    @JoinTable(
            name = "programme_cohorts",
            joinColumns = @JoinColumn(name = "programme_id"),
            inverseJoinColumns = @JoinColumn(name = "cohort_id")
    )
    @JsonIgnore
    private List<Cohort> cohorts;

    /** Semesters that structure this programme (Year 1 Sem 1, …). */
    @OneToMany(mappedBy = "programme")
    @JsonIgnore
    private List<Semester> semesters;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(255) default 'ACTIVE'")
    private ProgrammeStatus status = ProgrammeStatus.ACTIVE;

}
