package com.example.demo.usuario.model;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BajaUsuarioDTO {
    
    @NotBlank(message = "La contraseña es obligatoria para confirmar la operación")
    private String password;
    
    private boolean confirmacion;
}