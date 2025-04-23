package com.example.demo.usuario.control;

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

import com.example.demo.usuario.model.BajaUsuarioDTO;
import com.example.demo.usuario.model.CambiarPasswordDTO;
import com.example.demo.usuario.model.PerfilDTO;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.service.UsuarioService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/perfil")
public class PerfilController {

    @Autowired
    private UsuarioService usuarioService;
    
    /**
     * Muestra la página de perfil del usuario autenticado
     */
    @GetMapping
    public String mostrarPerfil(Model model) {
        // Obtener el usuario autenticado
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // Buscar el usuario en la base de datos
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        
        // Si no hay un PerfilDTO en el modelo (por ejemplo, después de un error), creamos uno nuevo
        if (!model.containsAttribute("perfilDTO")) {
            // Convertir el usuario a DTO para el formulario
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
        
        // Agregar información adicional al modelo si es necesario
        model.addAttribute("usuarioRoles", usuario.getRoles());
        
        return "usuario/perfil";
    }
    
    /**
     * Procesa la actualización del perfil
     */
    @PostMapping("/actualizar")
    public String actualizarPerfil(
            @Valid @ModelAttribute("perfilDTO") PerfilDTO perfilDTO,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        // Verificar errores de validación
        if (bindingResult.hasErrors()) {
            // Si hay errores, volver al formulario mostrando los mensajes
            return "usuario/perfil";
        }
        
        try {
            // Llamar al servicio para actualizar el perfil
            UsuarioVO usuarioActualizado = usuarioService.actualizarPerfil(perfilDTO);
            
            // Agregar mensaje de éxito para mostrar en la redirección
            redirectAttributes.addFlashAttribute("mensajeExito", 
                    "¡Tu perfil ha sido actualizado correctamente!");
            
            return "redirect:/perfil";
        } catch (Exception e) {
            // En caso de error, volver al formulario mostrando el mensaje de error
            model.addAttribute("mensajeError", "Error al actualizar el perfil: " + e.getMessage());
            return "usuario/perfil";
        }
    }
    
    /**
     * Muestra el formulario para cambiar la contraseña
     */
    @GetMapping("/cambiar-password")
    public String mostrarFormularioCambiarPassword(Model model) {
        // Si no hay un dto en el modelo (por ejemplo, después de un error), creamos uno nuevo
        if (!model.containsAttribute("cambiarPasswordDTO")) {
            model.addAttribute("cambiarPasswordDTO", new CambiarPasswordDTO());
        }
        
        return "usuario/cambiar-password";
    }

    /**
     * Procesa el cambio de contraseña
     */
    @PostMapping("/cambiar-password")
    public String cambiarPassword(
            @Valid @ModelAttribute("cambiarPasswordDTO") CambiarPasswordDTO cambiarPasswordDTO,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        // Verificar que las contraseñas nuevas coincidan
        if (!cambiarPasswordDTO.getPasswordNueva().equals(cambiarPasswordDTO.getConfirmPassword())) {
            bindingResult.addError(new FieldError("cambiarPasswordDTO", "confirmPassword", 
                "Las contraseñas no coinciden"));
        }
        
        // Verificar errores de validación
        if (bindingResult.hasErrors()) {
            return "usuario/cambiar-password";
        }
        
        try {
            // Llamar al servicio para cambiar la contraseña
            usuarioService.cambiarPassword(
                    cambiarPasswordDTO.getPasswordActual(),
                    cambiarPasswordDTO.getPasswordNueva());
            
            // Agregar mensaje de éxito para mostrar en la redirección
            redirectAttributes.addFlashAttribute("mensajeExito", 
                    "¡Tu contraseña ha sido actualizada correctamente!");
            
            return "redirect:/perfil";
        } catch (Exception e) {
            // En caso de error, volver al formulario mostrando el mensaje de error
            model.addAttribute("mensajeError", "Error al cambiar la contraseña: " + e.getMessage());
            return "usuario/cambiar-password";
        }
    }
    /**
     * Muestra el formulario para dar de baja al usuario
     */
    @GetMapping("/baja-usuario")
    public String mostrarFormularioBaja(Model model) {
        // Si no hay un dto en el modelo, creamos uno nuevo
        if (!model.containsAttribute("bajaUsuarioDTO")) {
            model.addAttribute("bajaUsuarioDTO", new BajaUsuarioDTO());
        }
        
        return "usuario/confirmar-baja";
    }

    /**
     * Procesa la solicitud de baja del usuario
     */
    @PostMapping("/baja-usuario")
    public String procesarBajaUsuario(
            @Valid @ModelAttribute("bajaUsuarioDTO") BajaUsuarioDTO bajaUsuarioDTO,
            BindingResult bindingResult,
            Model model,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        // Verificar que se ha confirmado la baja
        if (!bajaUsuarioDTO.isConfirmacion()) {
            bindingResult.rejectValue("confirmacion", "error.bajaUsuarioDTO", 
                                     "Debes confirmar que deseas dar de baja tu cuenta");
        }
        
        // Verificar errores de validación
        if (bindingResult.hasErrors()) {
            return "usuario/confirmar-baja";
        }
        
        try {
            // Llamar al servicio para dar de baja al usuario
            boolean resultado = usuarioService.darDeBajaUsuario(bajaUsuarioDTO.getPassword());
            
            if (resultado) {
                // Invalidar la sesión
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
                
                // Borrar las cookies de autenticación
                Cookie[] cookies = request.getCookies();
                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if (cookie.getName().equals("JSESSIONID")) {
                            cookie.setValue("");
                            cookie.setPath("/");
                            cookie.setMaxAge(0);
                            response.addCookie(cookie);
                        }
                    }
                }
                
                // Limpiar el contexto de seguridad
                SecurityContextHolder.clearContext();
                
                // Redireccionar a la página de confirmación
                return "redirect:/cuenta-eliminada";
            } else {
                model.addAttribute("mensajeError", "No se pudo dar de baja la cuenta. Intenta nuevamente.");
                return "usuario/confirmar-baja";
            }
        } catch (Exception e) {
            model.addAttribute("mensajeError", "Error al dar de baja la cuenta: " + e.getMessage());
            return "usuario/confirmar-baja";
        }
    }
}