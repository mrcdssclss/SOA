package org.example.model;


import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Embeddable
public class Location {
    @NotNull
    @Column(name = "location_x", nullable = false)
    private Long x; //Поле не может быть null

    @NotNull
    @Column(name = "location_y", nullable = false)
    private Float y; //Поле не может быть null

    @NotNull
    @Column(name = "location_z", nullable = false)
    private Integer z; //Поле не может быть null
}
