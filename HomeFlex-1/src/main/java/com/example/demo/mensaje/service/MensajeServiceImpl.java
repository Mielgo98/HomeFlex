package com.example.demo.mensaje.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.mensaje.model.ConversacionDTO;
import com.example.demo.mensaje.model.MensajeDTO;
import com.example.demo.mensaje.model.MensajeVO;
import com.example.demo.mensaje.repository.MensajeRepository;
import com.example.demo.notificacion.model.NotificacionVO;
import com.example.demo.notificacion.repository.NotificacionRepository;
import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.repository.PropiedadRepository;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.repository.UsuarioRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MensajeServiceImpl implements MensajeService {
    
    @Autowired
    private MensajeRepository mensajeRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PropiedadRepository propiedadRepository;
    
    @Autowired
    private NotificacionRepository notificacionRepository;
    
    @Override
    public List<ConversacionDTO> getConversaciones(Long usuarioId) {
        // Obtener receptores (usuarios a los que ha enviado mensajes)
        List<UsuarioVO> receptores = mensajeRepository.findReceptores(usuarioId);
        
        // Obtener emisores (usuarios que le han enviado mensajes)
        List<UsuarioVO> emisores = mensajeRepository.findEmisores(usuarioId);
        
        // Combinar los resultados eliminando duplicados
        Map<Long, UsuarioVO> contactosMap = new HashMap<>();
        
        for (UsuarioVO receptor : receptores) {
            contactosMap.put(receptor.getId(), receptor);
        }
        
        for (UsuarioVO emisor : emisores) {
            contactosMap.put(emisor.getId(), emisor);
        }
        
        // Convertir a lista
        List<UsuarioVO> contactos = new ArrayList<>(contactosMap.values());
        List<ConversacionDTO> conversaciones = new ArrayList<>();
        
        for (UsuarioVO contacto : contactos) {
            List<MensajeVO> mensajes = mensajeRepository.findConversacion(usuarioId, contacto.getId());
            
            // Obtener el último mensaje
            MensajeVO ultimoMensaje = !mensajes.isEmpty() ? mensajes.get(mensajes.size() - 1) : null;
            
            if (ultimoMensaje != null) {
                ConversacionDTO conversacion = new ConversacionDTO();
                conversacion.setContactoId(contacto.getId());
                conversacion.setContactoNombre(contacto.getNombre());
                conversacion.setContactoApellidos(contacto.getApellidos());
                conversacion.setContactoFoto(contacto.getFotoPerfil());
                
                // Mostrar contenido adecuado según el tipo de mensaje
                if ("texto".equals(ultimoMensaje.getTipoMensaje())) {
                    conversacion.setUltimoMensaje(ultimoMensaje.getContenido());
                } else if ("imagen".equals(ultimoMensaje.getTipoMensaje())) {
                    conversacion.setUltimoMensaje("[Imagen]");
                } else {
                    conversacion.setUltimoMensaje("[Mensaje]");
                }
                
                conversacion.setTipoUltimoMensaje(ultimoMensaje.getTipoMensaje());
                conversacion.setFechaUltimoMensaje(ultimoMensaje.getFechaEnvio());
                
                if (ultimoMensaje.getPropiedad() != null) {
                    conversacion.setPropiedadId(ultimoMensaje.getPropiedad().getId());
                    conversacion.setPropiedadTitulo(ultimoMensaje.getPropiedad().getTitulo());
                }
                
                // Contar mensajes no leídos
                Integer noLeidos = mensajeRepository.countUnreadMessagesFromUser(usuarioId, contacto.getId());
                conversacion.setMensajesNoLeidos(noLeidos);
                
                conversaciones.add(conversacion);
            }
        }
        
        // Ordenar por fecha del último mensaje (más reciente primero)
        conversaciones.sort(Comparator.comparing(ConversacionDTO::getFechaUltimoMensaje).reversed());
        
        return conversaciones;
    }
    
    @Override
    public List<MensajeDTO> getConversacion(Long usuarioId, Long otroUsuarioId, Long propiedadId) {
        List<MensajeVO> mensajes;
        
        if (propiedadId != null) {
            mensajes = mensajeRepository.findConversacionByPropiedad(usuarioId, otroUsuarioId, propiedadId);
        } else {
            mensajes = mensajeRepository.findConversacion(usuarioId, otroUsuarioId);
        }
        
        return mensajes.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    public MensajeDTO enviarMensaje(Long emisorId, Long receptorId, String contenido, Long propiedadId, String tipoMensaje, String urlRecurso) {
        UsuarioVO emisor = usuarioRepository.findById(emisorId)
                .orElseThrow(() -> new RuntimeException("Emisor no encontrado"));
        
        UsuarioVO receptor = usuarioRepository.findById(receptorId)
                .orElseThrow(() -> new RuntimeException("Receptor no encontrado"));
        
        PropiedadVO propiedad = null;
        if (propiedadId != null) {
            propiedad = propiedadRepository.findById(propiedadId)
                    .orElse(null);
        }
        
        MensajeVO mensaje = new MensajeVO();
        mensaje.setEmisor(emisor);
        mensaje.setReceptor(receptor);
        mensaje.setContenido(contenido);
        mensaje.setFechaEnvio(LocalDateTime.now());
        mensaje.setLeido(false);
        mensaje.setPropiedad(propiedad);
        mensaje.setTipoMensaje(tipoMensaje != null ? tipoMensaje : "texto");
        mensaje.setUrlRecurso(urlRecurso);
        
        MensajeVO mensajeGuardado = mensajeRepository.save(mensaje);
        
        // Crear notificación
        String mensajeNotificacion = "texto".equals(tipoMensaje) ? 
                "Nuevo mensaje de " + emisor.getNombre() + " " + emisor.getApellidos() :
                "Nuevo contenido de " + emisor.getNombre() + " " + emisor.getApellidos();
        
        NotificacionVO notificacion = new NotificacionVO();
        notificacion.setUsuario(receptor);
        notificacion.setTitulo("Nuevo mensaje");
        notificacion.setContenido(mensajeNotificacion);
        notificacion.setFechaCreacion(LocalDateTime.now());
        notificacion.setLeida(false);
        notificacion.setTipo("mensaje");
        notificacion.setLink("/mensajes/chat?contactoId=" + emisor.getId());
        
        notificacionRepository.save(notificacion);
        
        return convertToDTO(mensajeGuardado);
    }
    
    @Override
    @Transactional
    public void marcarComoLeidos(Long usuarioId, Long emisorId) {
        mensajeRepository.markAsRead(usuarioId, emisorId);
    }
    
    @Override
    public Integer contarNoLeidos(Long usuarioId) {
        return mensajeRepository.countUnreadMessages(usuarioId);
    }
    
    private MensajeDTO convertToDTO(MensajeVO mensaje) {
        MensajeDTO dto = new MensajeDTO();
        dto.setId(mensaje.getId());
        dto.setEmisorId(mensaje.getEmisor().getId());
        dto.setEmisorNombre(mensaje.getEmisor().getNombre());
        dto.setEmisorApellidos(mensaje.getEmisor().getApellidos());
        dto.setEmisorFoto(mensaje.getEmisor().getFotoPerfil());
        dto.setReceptorId(mensaje.getReceptor().getId());
        dto.setReceptorNombre(mensaje.getReceptor().getNombre());
        dto.setReceptorApellidos(mensaje.getReceptor().getApellidos());
        dto.setReceptorFoto(mensaje.getReceptor().getFotoPerfil());
        dto.setContenido(mensaje.getContenido());
        dto.setFechaEnvio(mensaje.getFechaEnvio());
        dto.setLeido(mensaje.getLeido());
        dto.setTipoMensaje(mensaje.getTipoMensaje());
        dto.setUrlRecurso(mensaje.getUrlRecurso());
        
        if (mensaje.getPropiedad() != null) {
            dto.setPropiedadId(mensaje.getPropiedad().getId());
            dto.setPropiedadTitulo(mensaje.getPropiedad().getTitulo());
        }
        
        return dto;
    }
}