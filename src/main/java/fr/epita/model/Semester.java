package fr.epita.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;

/** A semester within a programme, e.g. "Year 1 – Semester 1". */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "semesters")
public class Semester {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    /** Ordering within the programme (1, 2, 3 …). */
    @Column(nullable = false)
    private int orderIndex = 1;

    @ManyToOne
    @JoinColumn(name = "programme_id", nullable = false)
    private Programme programme;

    @OneToMany(mappedBy = "semester")
    @JsonIgnore
    private List<Course> courses;
}
