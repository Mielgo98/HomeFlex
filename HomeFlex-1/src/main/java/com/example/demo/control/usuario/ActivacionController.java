package com.example.demo.control.usuario;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.service.usuario.UsuarioService;

@Controller
public class ActivacionController {
    
    @Autowired
    private UsuarioService usuarioService;
    
    /**
     * Maneja la activación de cuentas mediante el token proporcionado en la URL.
     */
    @GetMapping("/activar")
    public String activarCuenta(@RequestParam("token") String token, Model model) {
        System.out.println("Intentando activar cuenta con token: " + token);
        
        boolean activacionExitosa = usuarioService.activarCuenta(token);
        
        if (activacionExitosa) {
            model.addAttribute("exito", true);
            model.addAttribute("mensaje", "¡Tu cuenta ha sido activada correctamente! Ahora puedes iniciar sesión.");
        } else {
            model.addAttribute("exito", false);
            model.addAttribute("mensaje", "El enlace de activación no es válido o ha expirado. Por favor, contacta con soporte.");
        }
        
        return "activacion-resultado";
    }
}