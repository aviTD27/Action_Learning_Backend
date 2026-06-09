package fr.epita.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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

    @OneToMany(mappedBy = "programme")
    @JsonIgnore
    private List<Lecturer> lecturers;


    // TODO Will need to link to university

}
