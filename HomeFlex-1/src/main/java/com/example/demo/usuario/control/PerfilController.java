package com.example.demo.usuario.control;

import jakarta.validation.Valid;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.email.service.EmailService;
import com.example.demo.usuario.model.BajaUsuarioDTO;
import com.example.demo.usuario.model.CambiarPasswordDTO;
import com.example.demo.usuario.model.PerfilDTO;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.service.UsuarioService;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    @Autowired
    private UsuarioService usuarioService;
    
    @Autowired
    private EmailService emailService;
    
    @GetMapping
    public String mostrarPerfil(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        
        if (!model.containsAttribute("perfilDTO")) {
            PerfilDTO perfilDTO = new PerfilDTO();
            perfilDTO.setId(usuario.getId());
            perfilDTO.setUsername(usuario.getUsername());
            perfilDTO.setEmail(usuario.getEmail());
            perfilDTO.setNombre(usuario.getNombre());
            perfilDTO.setApellidos(usuario.getApellidos());
            perfilDTO.setTelefono(usuario.getTelefono());
            perfilDTO.setFotoPerfil(usuario.getFotoPerfil());
            model.addAttribute("perfilDTO", perfilDTO);
        }

        model.addAttribute("usuarioRoles", usuario.getRoles());
        return "usuario/perfil";
    }
    
    @PostMapping("/actualizar")
    public String actualizarPerfil(
            @Valid @ModelAttribute("perfilDTO") PerfilDTO perfilDTO,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("usuarioRoles", usuarioService
                .buscarPorUsername(SecurityContextHolder.getContext()
                .getAuthentication().getName()).getRoles());
            return "usuario/perfil";
        }
        
        try {
            usuarioService.actualizarPerfil(perfilDTO);
            redirectAttributes.addFlashAttribute("mensajeExito",
                "¡Tu perfil ha sido actualizado correctamente!");
            return "redirect:/perfil";
        } catch (Exception e) {
            model.addAttribute("mensajeError", 
                "Error al actualizar el perfil: " + e.getMessage());
            model.addAttribute("usuarioRoles", usuarioService
                .buscarPorUsername(SecurityContextHolder.getContext()
                .getAuthentication().getName()).getRoles());
            return "usuario/perfil";
        }
    }
    
    @GetMapping("/cambiar-password")
    public String mostrarFormularioCambiarPassword(Model model) {
        if (!model.containsAttribute("cambiarPasswordDTO")) {
            model.addAttribute("cambiarPasswordDTO", new CambiarPasswordDTO());
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UsuarioVO usuario = usuarioService.buscarPorUsername(auth.getName());
        model.addAttribute("usuarioRoles", usuario.getRoles());
        
        return "usuario/cambiar-password";
    }

    @PostMapping("/cambiar-password")
    public String cambiarPassword(
            @Valid @ModelAttribute("cambiarPasswordDTO") CambiarPasswordDTO cambiarPasswordDTO,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) throws ServletException {
        
        // Validación de confirmación de contraseña
        if (!cambiarPasswordDTO.getPasswordNueva()
                .equals(cambiarPasswordDTO.getConfirmPassword())) {
            bindingResult.addError(new FieldError(
                "cambiarPasswordDTO", "confirmPassword", 
                "Las contraseñas no coinciden"));
        }
        
        if (bindingResult.hasErrors()) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UsuarioVO usuario = usuarioService.buscarPorUsername(auth.getName());
            model.addAttribute("usuarioRoles", usuario.getRoles());
            return "usuario/cambiar-password";
        }
        
        try {
            // Cambiar contraseña en base de datos
            usuarioService.cambiarPassword(
                cambiarPasswordDTO.getPasswordActual(),
                cambiarPasswordDTO.getPasswordNueva());
            
            // Notificar por email
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UsuarioVO usuario = usuarioService.buscarPorUsername(auth.getName());
            emailService.sendPasswordChangeEmail(usuario);
            
            // Hacer logout
            request.logout();
            
            // Redirigir a login con parámetro para mensaje
            return "redirect:/login?passwordChanged";
        } catch (Exception e) {
            model.addAttribute("mensajeError", 
                "Error al cambiar la contraseña: " + e.getMessage());
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            UsuarioVO usuario = usuarioService.buscarPorUsername(auth.getName());
            model.addAttribute("usuarioRoles", usuario.getRoles());
            return "usuario/cambiar-password";
        }
    }
    
    @GetMapping("/baja-usuario")
    public String mostrarFormularioBaja(Model model) {
        if (!model.containsAttribute("bajaUsuarioDTO")) {
            model.addAttribute("bajaUsuarioDTO", new BajaUsuarioDTO());
        }
        model.addAttribute("usuarioRoles", usuarioService
            .buscarPorUsername(SecurityContextHolder.getContext()
            .getAuthentication().getName()).getRoles());
        return "usuario/confirmar-baja";
    }

    @PostMapping("/baja-usuario")
    public String procesarBajaUsuario(
        @Valid @ModelAttribute("bajaUsuarioDTO") BajaUsuarioDTO bajaUsuarioDTO,
        BindingResult bindingResult,
        Model model,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws ServletException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = (auth != null) ? auth.getName() : null;

        if (!bajaUsuarioDTO.isConfirmacion()) {
            bindingResult.rejectValue("confirmacion", "error.bajaUsuarioDTO", 
                "Debes confirmar que deseas dar de baja tu cuenta");
        }

        if (bindingResult.hasErrors()) {
            if (username != null)
                model.addAttribute("usuarioRoles", usuarioService.buscarPorUsername(username).getRoles());
            return "usuario/confirmar-baja";
        }

        try {
            boolean resultado = usuarioService.darDeBajaUsuario(bajaUsuarioDTO.getPassword());

            if (resultado) {
                HttpSession session = request.getSession(false);
                if (session != null) session.invalidate();
                if (request.getCookies() != null) {
                    for (Cookie cookie : request.getCookies()) {
                        if ("JSESSIONID".equals(cookie.getName())) {
                            cookie.setValue("");
                            cookie.setPath("/");
                            cookie.setMaxAge(0);
                            response.addCookie(cookie);
                        }
                    }
                }
                SecurityContextHolder.clearContext();
                return "redirect:/cuenta-eliminada";
            } else {
                model.addAttribute("mensajeError", "No se pudo dar de baja la cuenta. Intenta nuevamente.");
                if (username != null)
                    model.addAttribute("usuarioRoles", usuarioService.buscarPorUsername(username).getRoles());
                return "usuario/confirmar-baja";
            }
        } catch (Exception e) {
            model.addAttribute("mensajeError", "Error al dar de baja la cuenta: " + e.getMessage());
            if (username != null)
                model.addAttribute("usuarioRoles", usuarioService.buscarPorUsername(username).getRoles());
            return "usuario/confirmar-baja";
        }
    }

}
