package com.example.demo.mensaje.service;



import java.util.List;

import com.example.demo.mensaje.model.ConversacionDTO;
import com.example.demo.mensaje.model.MensajeDTO;

public interface MensajeService {
    
    List<ConversacionDTO> getConversaciones(Long usuarioId);
    
    List<MensajeDTO> getConversacion(Long usuarioId, Long otroUsuarioId, Long propiedadId);
    
    MensajeDTO enviarMensaje(Long emisorId, Long receptorId, String contenido, Long propiedadId, String tipoMensaje, String urlRecurso);
    
    void marcarComoLeidos(Long usuarioId, Long emisorId);
    
    Integer contarNoLeidos(Long usuarioId);
}