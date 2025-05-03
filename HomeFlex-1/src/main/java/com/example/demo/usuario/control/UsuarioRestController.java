package com.example.demo.usuario.control;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.propiedad.service.PropiedadService;
import com.example.demo.propiedad.model.PropiedadDTO;
import com.example.demo.usuario.service.UsuarioService;

@RestController
@RequestMapping("/api/usuario")
public class UsuarioRestController {

    @Autowired
    private PropiedadService propiedadService;
    
    @Autowired
    private UsuarioService usuarioService;
    
    /**
     * Obtiene las propiedades favoritas del usuario autenticado
     */
    @GetMapping("/propiedades-favoritas")
    public ResponseEntity<List<PropiedadDTO>> obtenerPropiedadesFavoritas(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            String username = principal.getName();
            List<PropiedadDTO> favoritos = usuarioService.obtenerPropiedadesFavoritas(username);
            return ResponseEntity.ok(favoritos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Elimina una propiedad de los favoritos del usuario
     */
    @DeleteMapping("/eliminar-favorito/{propiedadId}")
    public ResponseEntity<Map<String, Object>> eliminarFavorito(
            @PathVariable Long propiedadId, 
            Principal principal) {
        
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            String username = principal.getName();
            usuarioService.eliminarPropiedadFavorita(username, propiedadId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Propiedad eliminada de favoritos correctamente");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al eliminar la propiedad de favoritos: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
    
    /**
     * Añade una propiedad a los favoritos del usuario
     */
    @PostMapping("/agregar-favorito/{propiedadId}")
    public ResponseEntity<Map<String, Object>> agregarFavorito(
            @PathVariable Long propiedadId, 
            Principal principal) {
        
        if (principal == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        
        try {
            String username = principal.getName();
            usuarioService.agregarPropiedadFavorita(username, propiedadId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Propiedad añadida a favoritos correctamente");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al añadir la propiedad a favoritos: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}