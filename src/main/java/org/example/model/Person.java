package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDate;

@Data
@Entity
@Table(name="people")
public class Person {
    @Min(1)
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; //Поле не может быть null, Значение поля должно быть больше 0, Значение этого поля должно быть уникальным, Значение этого поля должно генерироваться автоматически

    @NotEmpty
    private String name; //Поле не может быть null, Строка не может быть пустой

    @NotNull
    @Valid
    @Embedded
    private Coordinates coordinates; //Поле не может быть null

    @Column(name = "creation_date", nullable = false, updatable = false)
    @NotNull
    private LocalDate creationDate; //Поле не может быть null, Значение этого поля должно генерироваться автоматически

    @NotNull
    @Positive
    private Integer height; //Поле не может быть null, Значение поля должно быть больше 0

    @NotNull
    @Enumerated(EnumType.STRING)
    private EyeColor eyeColor; //Поле не может быть null

    @NotNull
    @Enumerated(EnumType.STRING)
    private HairColor hairColor; //Поле не может быть null

    @Enumerated(EnumType.STRING)
    private Country nationality; //Поле может быть null

    @NotNull
    @Valid
    @Embedded
    private Location location; //Поле не может быть null


    @PrePersist
    private void beforeSave() {
        creationDate = LocalDate.now();
    }
}
