package com.example.demo.control.usuario;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.model.usuario.LoginDTO;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String mostrarLogin(
            @RequestParam(value = "error", required = false) String error,
            @RequestParam(value = "logout", required = false) String logout,
            @RequestParam(value = "registroExitoso", required = false) String registroExitoso,
            @RequestParam(value = "disabled", required = false) String disabled,
            Model model) {
        
        System.out.println("LoginController: Mostrando página de login");
        
        if (error != null) {
            model.addAttribute("errorMsg", "Credenciales inválidas");
        }

        if (logout != null) {
            model.addAttribute("logoutMsg", "Has cerrado sesión correctamente");
        }
        
        if (registroExitoso != null) {
            model.addAttribute("registroExitosoMsg", "¡Registro exitoso! Por favor, revisa tu correo electrónico para activar tu cuenta.");
        }
        
        if (disabled != null) {
            model.addAttribute("disabledMsg", "Tu cuenta no está activada. Por favor, revisa tu correo electrónico y sigue las instrucciones para activarla.");
        }
        
        model.addAttribute("loginDTO", new LoginDTO());
        return "loginPage";
    }
}