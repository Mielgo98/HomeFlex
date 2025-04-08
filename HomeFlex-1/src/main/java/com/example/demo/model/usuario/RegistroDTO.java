package com.example.demo.model.usuario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO específico para el proceso de registro de usuarios
 * Contiene todos los campos necesarios para crear un nuevo usuario
 * con sus respectivas validaciones
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroDTO {
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    @Size(max = 100, message = "El email no puede exceder los 100 caracteres")
    private String email;
    
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,}$", 
            message = "La contraseña debe contener al menos un número, una mayúscula, una minúscula y un caracter especial")
    private String password;
    
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
    
    
    
}