package com.example.demo.usuario.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO específico para actualizar datos de perfil del usuario
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerfilDTO {
    
    // Campo oculto para el ID - no editable por el usuario
    private Long id;
    
    // Campos no modificables - solo para mostrar
    private String username;
    private String email;
    
    // Campos que el usuario puede actualizar
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre;
    
    @NotBlank(message = "Los apellidos son obligatorios")
    @Size(min = 2, max = 100, message = "Los apellidos deben tener entre 2 y 100 caracteres")
    private String apellidos;
    
    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = "^(6|7|8|9)([0-9]){8}$", 
            message = "Formato de teléfono español inválido")
    private String telefono;
    
    // Campo opcional para cargar una foto de perfil
    private String fotoPerfil;
}