package com.example.demo.mensaje.control;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.mensaje.model.ConversacionDTO;
import com.example.demo.mensaje.model.MensajeDTO;
import com.example.demo.mensaje.service.MensajeService;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.service.UsuarioService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/mensajes")
public class MensajeRestController {
    
    @Autowired
    private MensajeService mensajeService;
    
    @Autowired
    private UsuarioService usuarioService;
    
    // Ruta donde se guardarán las imágenes enviadas
    private final String UPLOAD_DIR = "src/main/resources/static/uploads/mensajes/";
    
    @GetMapping
    public ResponseEntity<List<ConversacionDTO>> getConversaciones() {
        // Obtener el usuario autenticado del contexto de seguridad
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // Buscar el usuario usando el servicio
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        Long usuarioId = usuario.getId();
        
        List<ConversacionDTO> conversaciones = mensajeService.getConversaciones(usuarioId);
        return ResponseEntity.ok(conversaciones);
    }
    
    @GetMapping("/{otroUsuarioId}")
    public ResponseEntity<List<MensajeDTO>> getConversacion(
            @PathVariable Long otroUsuarioId,
            @RequestParam(required = false) Long propiedadId) {
        
        // Obtener el usuario autenticado del contexto de seguridad
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // Buscar el usuario usando el servicio
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        Long usuarioId = usuario.getId();
        
        // Marcar mensajes como leídos
        mensajeService.marcarComoLeidos(usuarioId, otroUsuarioId);
        
        List<MensajeDTO> mensajes = mensajeService.getConversacion(usuarioId, otroUsuarioId, propiedadId);
        return ResponseEntity.ok(mensajes);
    }
    
    @PostMapping
    public ResponseEntity<MensajeDTO> enviarMensaje(@RequestBody Map<String, Object> payload) {
        // Obtener el usuario autenticado del contexto de seguridad
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // Buscar el usuario usando el servicio
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        Long usuarioId = usuario.getId();
        
        Long receptorId = Long.valueOf(payload.get("receptorId").toString());
        String contenido = (String) payload.get("contenido");
        Long propiedadId = payload.containsKey("propiedadId") ? 
                Long.valueOf(payload.get("propiedadId").toString()) : null;
        String tipoMensaje = payload.containsKey("tipoMensaje") ? 
                (String) payload.get("tipoMensaje") : "texto";
        String urlRecurso = payload.containsKey("urlRecurso") ? 
                (String) payload.get("urlRecurso") : null;
        
        MensajeDTO mensaje = mensajeService.enviarMensaje(usuarioId, receptorId, contenido, propiedadId, tipoMensaje, urlRecurso);
        return ResponseEntity.status(HttpStatus.CREATED).body(mensaje);
    }
    
    @PostMapping("/imagen")
    public ResponseEntity<Map<String, String>> enviarImagen(
            @RequestParam("imagen") MultipartFile file,
            @RequestParam("receptorId") Long receptorId,
            @RequestParam(value = "propiedadId", required = false) Long propiedadId) {
        
        // Obtener el usuario autenticado del contexto de seguridad
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // Buscar el usuario usando el servicio
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        Long usuarioId = usuario.getId();
        
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Archivo vacío"));
        }
        
        try {
            // Crear directorio si no existe
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            // Generar nombre único para el archivo
            String originalFileName = file.getOriginalFilename();
            String fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String newFileName = UUID.randomUUID().toString() + fileExtension;
            
            // Guardar archivo
            Path filePath = uploadPath.resolve(newFileName);
            Files.copy(file.getInputStream(), filePath);
            
            // URL relativa del recurso para acceder desde el navegador
            String urlRecurso = "/uploads/mensajes/" + newFileName;
            
            // Enviar mensaje con la imagen
            MensajeDTO mensaje = mensajeService.enviarMensaje(
                    usuarioId, 
                    receptorId, 
                    "Imagen compartida", 
                    propiedadId, 
                    "imagen", 
                    urlRecurso);
            
            Map<String, String> response = new HashMap<>();
            response.put("url", urlRecurso);
            response.put("mensajeId", mensaje.getId().toString());
            
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al guardar la imagen: " + e.getMessage()));
        }
    }
    
    @GetMapping("/no-leidos")
    public ResponseEntity<Map<String, Integer>> contarNoLeidos() {
        // Obtener el usuario autenticado del contexto de seguridad
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        // Buscar el usuario usando el servicio
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        Long usuarioId = usuario.getId();
        
        Integer total = mensajeService.contarNoLeidos(usuarioId);
        Map<String, Integer> respuesta = new HashMap<>();
        respuesta.put("total", total);
        
        return ResponseEntity.ok(respuesta);
    }
}