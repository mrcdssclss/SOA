package org.example.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Embeddable
public class Coordinates {
    @NotNull
    @Max(557)
    @Column(name = "coordinates_x", nullable = false)
    private Long x; //Максимальное значение поля: 557, Поле не может быть null

    @Column(name = "coordinates_y", nullable = false)
    private double y;
}