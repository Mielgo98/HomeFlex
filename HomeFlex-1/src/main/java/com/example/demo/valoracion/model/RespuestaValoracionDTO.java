package com.example.demo.valoracion.model;


/**
 * DTO para responder a una valoración (por parte del propietario)
 */

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RespuestaValoracionDTO {
    
    @NotNull(message = "El ID de la valoración es obligatorio")
    private Long valoracionId;
    
    @NotNull(message = "La respuesta es obligatoria")
    @Size(min = 10, max = 1000, message = "La respuesta debe tener entre 10 y 1000 caracteres")
    private String respuesta;
}