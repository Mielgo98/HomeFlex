package com.example.demo.usuario.control;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.usuario.model.RegistroDTO;
import com.example.demo.usuario.service.UsuarioService;

import jakarta.validation.Valid;

@Controller
public class RegistroController {
     
    @Autowired
    private UsuarioService usuarioService;
    
    @GetMapping("/registro")
    public String mostrarFormularioRegistro(Model model) {
        if (!model.containsAttribute("registroDTO")) {
            model.addAttribute("registroDTO", new RegistroDTO());
        }
        return "registro"; 
    }
    
    @PostMapping("/registro")
    public String registrarUsuario(
            @Valid @ModelAttribute("registroDTO") RegistroDTO registroDTO,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        // Log para depuración
        System.out.println("Datos recibidos: " + registroDTO);
        
        // Validación manual para comprobar si las contraseñas coinciden
        if (!registroDTO.getPassword().equals(registroDTO.getConfirmPassword())) {
            bindingResult.addError(new FieldError("registroDTO", "confirmPassword", 
                registroDTO.getConfirmPassword(), false, null, null,
                "Las contraseñas no coinciden"));
        }
        
        if (bindingResult.hasErrors()) {
            System.out.println("Errores de validación: " + bindingResult.getAllErrors());
            // Agregar todos los errores como atributos para mostrarlos en la vista
            bindingResult.getAllErrors().forEach(error -> {
                if (error instanceof FieldError) {
                    FieldError fieldError = (FieldError) error;
                    model.addAttribute(fieldError.getField() + "Error", fieldError.getDefaultMessage());
                }
            });
            return "registro";
        }
        
        try {
            // Llama a tu servicio para guardar el usuario
            usuarioService.registroUsuario(registroDTO);
            redirectAttributes.addFlashAttribute("registroExitoso", true);
            return "redirect:/login?registroExitoso";
        } catch (Exception e) {
            System.err.println("Error en el proceso de registro: " + e.getMessage());
            if (e.getMessage().contains("username")) {
                model.addAttribute("usernameError", "El nombre de usuario ya está en uso");
            } else if (e.getMessage().contains("email")) {
                model.addAttribute("emailError", "El email ya está registrado");
            } else {
                model.addAttribute("errorRegistro", "Error al registrar: " + e.getMessage());
            }
            return "registro";
        }
    }
}