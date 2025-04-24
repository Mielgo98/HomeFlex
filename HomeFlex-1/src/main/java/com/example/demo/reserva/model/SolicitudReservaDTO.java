package com.example.demo.reserva.model;

import java.time.LocalDate;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la solicitud de una nueva reserva
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudReservaDTO {
    
    @NotNull(message = "La propiedad es obligatoria")
    private Long propiedadId;
    
    @NotNull(message = "La fecha de inicio es obligatoria")
    @Future(message = "La fecha de inicio debe ser en el futuro")
    private LocalDate fechaInicio;
    
    @NotNull(message = "La fecha de fin es obligatoria")
    @Future(message = "La fecha de fin debe ser en el futuro")
    private LocalDate fechaFin;
    
    @NotNull(message = "El número de huéspedes es obligatorio")
    @Min(value = 1, message = "Debe haber al menos un huésped")
    private Integer numHuespedes;
    
    private String comentarios;
}