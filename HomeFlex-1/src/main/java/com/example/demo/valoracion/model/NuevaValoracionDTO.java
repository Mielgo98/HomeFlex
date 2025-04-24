package com.example.demo.valoracion.model;


/**
 * DTO para crear una nueva valoración
 */

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
public class NuevaValoracionDTO {
    
    @NotNull(message = "La propiedad es obligatoria")
    private Long propiedadId;
    
    @NotNull(message = "La puntuación general es obligatoria")
    @Min(value = 1, message = "La puntuación mínima es 1")
    @Max(value = 5, message = "La puntuación máxima es 5")
    private Integer puntuacion;
    
    @Size(max = 1000, message = "El comentario no puede exceder los 1000 caracteres")
    private String comentario;
    
    @Min(value = 1, message = "La puntuación mínima es 1")
    @Max(value = 5, message = "La puntuación máxima es 5")
    private Integer limpieza;
    
    @Min(value = 1, message = "La puntuación mínima es 1")
    @Max(value = 5, message = "La puntuación máxima es 5")
    private Integer ubicacion;
    
    @Min(value = 1, message = "La puntuación mínima es 1")
    @Max(value = 5, message = "La puntuación máxima es 5")
    private Integer comunicacion;
    
    @Min(value = 1, message = "La puntuación mínima es 1")
    @Max(value = 5, message = "La puntuación máxima es 5")
    private Integer calidad;
    
    // ID de la reserva relacionada con esta valoración (opcional)
    private Long reservaId;
}