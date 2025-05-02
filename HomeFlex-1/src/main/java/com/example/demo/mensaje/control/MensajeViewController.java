package com.example.demo.mensaje.control;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.repository.PropiedadRepository;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.repository.UsuarioRepository;

@Controller
@RequestMapping("/mensajes")
public class MensajeViewController {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PropiedadRepository propiedadRepository;
    
    @GetMapping("/lista")
    public String listaMensajes(Model model, Authentication authentication) {
        // Aquí solo renderizamos la vista, los datos se cargan con JavaScript
        return "mensajes/lista";
    }
    
    @GetMapping("/chat")
    public String chat(
            @RequestParam Long contactoId,
            @RequestParam(required = false) Long propiedadId,
            Model model, 
            Authentication authentication) {
        
        // Verificar que el contacto existe
        if (!usuarioRepository.existsById(contactoId)) {
            return "redirect:/mensajes/lista";
        }
        
        // Verificar que la propiedad existe si se ha proporcionado
        if (propiedadId != null && !propiedadRepository.existsById(propiedadId)) {
            return "redirect:/mensajes/lista";
        }
        
        // Obtener información del contacto para el título
        UsuarioVO contacto = usuarioRepository.findById(contactoId).orElse(null);
        if (contacto != null) {
            model.addAttribute("contacto", contacto);
        }
        
        // Obtener información de la propiedad si existe
        if (propiedadId != null) {
            PropiedadVO propiedad = propiedadRepository.findById(propiedadId).orElse(null);
            if (propiedad != null) {
                model.addAttribute("propiedad", propiedad);
            }
        }
        
        // Los datos principales se cargarán con JavaScript
        return "mensajes/chat";
    }
    
    @GetMapping("/nuevo")
    public String nuevoMensaje(
            @RequestParam Long contactoId,
            @RequestParam(required = false) Long propiedadId,
            Model model, 
            Authentication authentication) {
        
        // Redirigir a la conversación existente
        return "redirect:/mensajes/chat?contactoId=" + contactoId + 
               (propiedadId != null ? "&propiedadId=" + propiedadId : "");
    }
}