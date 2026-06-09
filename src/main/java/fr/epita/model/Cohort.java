package fr.epita.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fr.epita.enums.CohortStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "cohorts")
public class Cohort {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "programme_id", nullable = false)
    private Programme programme;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CohortStatus status = CohortStatus.NOT_STARTED;

    @OneToMany(mappedBy = "cohort")
    @JsonIgnore
    private List<Student> students;

    // TODO Add University relationship

}

