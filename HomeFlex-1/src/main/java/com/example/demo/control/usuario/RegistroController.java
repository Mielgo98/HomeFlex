package com.example.demo.control.usuario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.model.usuario.RegistroDTO;
import com.example.demo.service.usuario.UsuarioService;

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
        
        if (bindingResult.hasErrors()) {
            System.out.println("Errores de validación: " + bindingResult.getAllErrors());
            return "registro";
        }
        
        try {
            // Llama a tu servicio para guardar el usuario
            usuarioService.registroUsuario(registroDTO);
            redirectAttributes.addFlashAttribute("registroExitoso", true);
            return "redirect:/login?registroExitoso";
        } catch (Exception e) {
            System.err.println("Error en el proceso de registro: " + e.getMessage());
            model.addAttribute("errorRegistro", "Error al registrar: " + e.getMessage());
            return "registro";
        }
    }
}