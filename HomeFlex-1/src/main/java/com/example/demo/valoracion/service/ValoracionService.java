package com.example.demo.valoracion.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.email.service.EmailService;
import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.service.PropiedadService;
import com.example.demo.reserva.model.ReservaVO;
import com.example.demo.reserva.service.ReservaService;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.service.UsuarioService;
import com.example.demo.valoracion.model.NuevaValoracionDTO;
import com.example.demo.valoracion.model.RespuestaValoracionDTO;
import com.example.demo.valoracion.model.ValoracionDTO;
import com.example.demo.valoracion.model.ValoracionVO;
import com.example.demo.valoracion.repository.ValoracionRepository;

import jakarta.mail.MessagingException;

@Service
public class ValoracionService {

    @Autowired
    private ValoracionRepository valoracionRepository;
    
    @Autowired
    private UsuarioService usuarioService;
    
    @Autowired
    private PropiedadService propiedadService;
    
    @Autowired
    private ReservaService reservaService;
    
    @Autowired
    private EmailService emailService;
    
    /**
     * Crea una nueva valoración
     */
    @Transactional
    public ValoracionDTO crearValoracion(NuevaValoracionDTO dto, String username) {
        // Obtener el usuario que hace la valoración
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        
        // Obtener la propiedad valorada
        PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(dto.getPropiedadId());
        
        // Verificar que el usuario ha tenido una reserva confirmada en esta propiedad
        // (opcional) Si se proporciona reservaId, verificar que esa reserva existe y pertenece al usuario
        if (dto.getReservaId() != null) {
            ReservaVO reserva = reservaService.obtenerReservaCompletaPorId(dto.getReservaId());
            if (!reserva.getUsuario().getId().equals(usuario.getId()) || 
                !reserva.getPropiedad().getId().equals(propiedad.getId())) {
                throw new RuntimeException("La reserva especificada no es válida para esta valoración");
            }
        }
        
        // Verificar si el usuario ya ha valorado esta propiedad
        Optional<ValoracionVO> valoracionExistente = valoracionRepository.findByPropiedadAndUsuario(propiedad, usuario);
        if (valoracionExistente.isPresent()) {
            throw new RuntimeException("Ya has valorado esta propiedad anteriormente");
        }
        
        // Crear la nueva valoración
        ValoracionVO valoracion = new ValoracionVO();
        valoracion.setPropiedad(propiedad);
        valoracion.setUsuario(usuario);
        valoracion.setPuntuacion(dto.getPuntuacion());
        valoracion.setComentario(dto.getComentario());
        valoracion.setFechaCreacion(LocalDateTime.now());
        valoracion.setAprobada(true); // Por defecto las valoraciones se aprueban automáticamente
        valoracion.setLimpieza(dto.getLimpieza());
        valoracion.setUbicacion(dto.getUbicacion());
        valoracion.setComunicacion(dto.getComunicacion());
        valoracion.setCalidad(dto.getCalidad());
        
        // Guardar la valoración
        ValoracionVO valoracionGuardada = valoracionRepository.save(valoracion);
        
        // Enviar notificación al propietario
        try {
            emailService.enviarNotificacionNuevaValoracion(
                    propiedad.getPropietario().getEmail(),
                    propiedad.getPropietario().getNombre(),
                    usuario.getNombre(),
                    propiedad.getTitulo(),
                    valoracionGuardada.getPuntuacion(),
                    valoracionGuardada.getComentario()
            );
        } catch (MessagingException e) {
            // Log del error pero continuamos con el proceso
            System.err.println("Error al enviar notificación de valoración: " + e.getMessage());
        }
        
        return new ValoracionDTO(valoracionGuardada);
    }
    
    /**
     * Actualiza una valoración existente
     */
    @Transactional
    public ValoracionDTO actualizarValoracion(Long id, NuevaValoracionDTO dto, String username) {
        // Obtener la valoración existente
        ValoracionVO valoracion = valoracionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Valoración no encontrada"));
        
        // Verificar que el usuario que actualiza es el mismo que creó la valoración
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        if (!valoracion.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("No tienes permiso para modificar esta valoración");
        }
        
        // Actualizar los campos
        valoracion.setPuntuacion(dto.getPuntuacion());
        valoracion.setComentario(dto.getComentario());
        valoracion.setLimpieza(dto.getLimpieza());
        valoracion.setUbicacion(dto.getUbicacion());
        valoracion.setComunicacion(dto.getComunicacion());
        valoracion.setCalidad(dto.getCalidad());
        
        // Las valoraciones actualizadas requieren aprobación nuevamente
        valoracion.setAprobada(false);
        
        // Guardar la valoración actualizada
        ValoracionVO valoracionActualizada = valoracionRepository.save(valoracion);
        
        return new ValoracionDTO(valoracionActualizada);
    }
    
    /**
     * Elimina una valoración
     */
    @Transactional
    public void eliminarValoracion(Long id, String username) {
        // Obtener la valoración
        ValoracionVO valoracion = valoracionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Valoración no encontrada"));
        
        // Verificar los permisos (el propietario de la valoración o el administrador pueden eliminarla)
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        boolean esAutor = valoracion.getUsuario().getId().equals(usuario.getId());
        boolean esAdmin = usuario.getRoles().stream().anyMatch(r -> r.getNombre().equals("ADMIN"));
        
        if (!esAutor && !esAdmin) {
            throw new RuntimeException("No tienes permiso para eliminar esta valoración");
        }
        
        // Eliminar la valoración
        valoracionRepository.delete(valoracion);
    }
    
    /**
     * Obtiene una valoración por su ID
     */
    public ValoracionDTO obtenerValoracionPorId(Long id) {
        ValoracionVO valoracion = valoracionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Valoración no encontrada"));
        
        return new ValoracionDTO(valoracion);
    }
    
    /**
     * Obtiene todas las valoraciones de una propiedad
     */
    public List<ValoracionDTO> obtenerValoracionesPorPropiedad(Long propiedadId) {
        PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(propiedadId);
        
        return valoracionRepository.findByPropiedadAndAprobadaTrue(propiedad)
                .stream()
                .map(ValoracionDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene todas las valoraciones de una propiedad con paginación
     */
    public Page<ValoracionDTO> obtenerValoracionesPorPropiedadPaginadas(Long propiedadId, Pageable pageable) {
        PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(propiedadId);
        
        Page<ValoracionVO> valoraciones = valoracionRepository.findByPropiedadAndAprobadaTrue(propiedad, pageable);
        
        return valoraciones.map(ValoracionDTO::new);
    }
    
    /**
     * Obtiene todas las valoraciones hechas por un usuario
     */
    public List<ValoracionDTO> obtenerValoracionesPorUsuario(String username) {
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        
        return valoracionRepository.findByUsuario(usuario)
                .stream()
                .map(ValoracionDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene todas las valoraciones de las propiedades de un propietario
     */
    public List<ValoracionDTO> obtenerValoracionesPorPropietario(String username) {
        UsuarioVO propietario = usuarioService.buscarPorUsername(username);
        
        return valoracionRepository.findByPropietario(propietario)
                .stream()
                .map(ValoracionDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene todas las valoraciones de las propiedades de un propietario con paginación
     */
    public Page<ValoracionDTO> obtenerValoracionesPorPropietarioPaginadas(String username, Pageable pageable) {
        UsuarioVO propietario = usuarioService.buscarPorUsername(username);
        
        Page<ValoracionVO> valoraciones = valoracionRepository.findByPropietario(propietario, pageable);
        
        return valoraciones.map(ValoracionDTO::new);
    }
    
    /**
     * Obtiene todas las valoraciones pendientes de aprobación
     */
    public List<ValoracionDTO> obtenerValoracionesPendientes() {
        return valoracionRepository.findAll()
                .stream()
                .filter(v -> !v.getAprobada())
                .map(ValoracionDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Aprueba una valoración
     */
    @Transactional
    public ValoracionDTO aprobarValoracion(Long id, String username) {
        // Verificar que el usuario es administrador
        UsuarioVO admin = usuarioService.buscarPorUsername(username);
        boolean esAdmin = admin.getRoles().stream().anyMatch(r -> r.getNombre().equals("ADMIN"));
        
        if (!esAdmin) {
            throw new RuntimeException("No tienes permisos para aprobar valoraciones");
        }
        
        // Obtener la valoración
        ValoracionVO valoracion = valoracionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Valoración no encontrada"));
        
        // Aprobar la valoración
        valoracion.setAprobada(true);
        ValoracionVO valoracionAprobada = valoracionRepository.save(valoracion);
        
        return new ValoracionDTO(valoracionAprobada);
    }
    
    /**
     * Rechaza una valoración
     */
    @Transactional
    public void rechazarValoracion(Long id, String username) {
        // Verificar que el usuario es administrador
        UsuarioVO admin = usuarioService.buscarPorUsername(username);
        boolean esAdmin = admin.getRoles().stream().anyMatch(r -> r.getNombre().equals("ADMIN"));
        
        if (!esAdmin) {
            throw new RuntimeException("No tienes permisos para rechazar valoraciones");
        }
        
        // Obtener la valoración
        ValoracionVO valoracion = valoracionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Valoración no encontrada"));
        
        // Eliminar la valoración
        valoracionRepository.delete(valoracion);
    }
    
    /**
     * Responde a una valoración (como propietario)
     */
    @Transactional
    public ValoracionDTO responderValoracion(RespuestaValoracionDTO dto, String username) {
        // Obtener la valoración
        ValoracionVO valoracion = valoracionRepository.findById(dto.getValoracionId())
                .orElseThrow(() -> new RuntimeException("Valoración no encontrada"));
        
        // Verificar que el usuario es el propietario de la propiedad
        UsuarioVO propietario = usuarioService.buscarPorUsername(username);
        if (!valoracion.getPropiedad().getPropietario().getId().equals(propietario.getId())) {
            throw new RuntimeException("No tienes permiso para responder a esta valoración");
        }
        
        // Añadir la respuesta
        valoracion.setRespuestaPropietario(dto.getRespuesta());
        valoracion.setFechaRespuesta(LocalDateTime.now());
        
        // Guardar la valoración con la respuesta
        ValoracionVO valoracionRespondida = valoracionRepository.save(valoracion);
        
        // Notificar al usuario que ha recibido una respuesta
        try {
            emailService.enviarNotificacionRespuestaValoracion(
                    valoracion.getUsuario().getEmail(),
                    valoracion.getUsuario().getNombre(),
                    propietario.getNombre(),
                    valoracion.getPropiedad().getTitulo(),
                    dto.getRespuesta()
            );
        } catch (MessagingException e) {
            // Log del error pero continuamos con el proceso
            System.err.println("Error al enviar notificación de respuesta: " + e.getMessage());
        }
        
        return new ValoracionDTO(valoracionRespondida);
    }
    
    /**
     * Calcula la puntuación media de una propiedad
     */
    public Double calcularPuntuacionMediaPropiedad(Long propiedadId) {
        PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(propiedadId);
        
        Double puntuacion = valoracionRepository.calcularPuntuacionMediaPropiedad(propiedad);
        
        return puntuacion != null ? puntuacion : 0.0;
    }
    
    /**
     * Calcula las puntuaciones medias detalladas de una propiedad
     */
    public ValoracionesEstadisticasDTO calcularEstadisticasPropiedad(Long propiedadId) {
        PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(propiedadId);
        
        ValoracionesEstadisticasDTO estadisticas = new ValoracionesEstadisticasDTO();
        estadisticas.setPropiedadId(propiedadId);
        estadisticas.setPuntuacionGeneral(valoracionRepository.calcularPuntuacionMediaPropiedad(propiedad));
        estadisticas.setPuntuacionLimpieza(valoracionRepository.calcularPuntuacionMediaLimpieza(propiedad));
        estadisticas.setPuntuacionUbicacion(valoracionRepository.calcularPuntuacionMediaUbicacion(propiedad));
        estadisticas.setPuntuacionComunicacion(valoracionRepository.calcularPuntuacionMediaComunicacion(propiedad));
        estadisticas.setPuntuacionCalidad(valoracionRepository.calcularPuntuacionMediaCalidad(propiedad));
        estadisticas.setNumeroValoraciones((int) valoracionRepository.countByPropiedadAndAprobadaTrue(propiedad));
        
        return estadisticas;
    }
    
    /**
     * Obtiene las valoraciones pendientes de respuesta para un propietario
     */
    public List<ValoracionDTO> obtenerValoracionesPendientesRespuesta(String username) {
        UsuarioVO propietario = usuarioService.buscarPorUsername(username);
        
        return valoracionRepository.findByPropietario(propietario)
                .stream()
                .filter(v -> v.getAprobada() && v.getRespuestaPropietario() == null)
                .map(ValoracionDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene las valoraciones filtradas por propiedad y aprobación
     */
    public Page<ValoracionDTO> filtrarValoraciones(
            Long propiedadId, 
            Boolean aprobada, 
            Pageable pageable) {
        
        PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(propiedadId);
        
        Page<ValoracionVO> valoraciones = valoracionRepository.findByPropiedadAndAprobada(propiedad, aprobada, pageable);
        
        return valoraciones.map(ValoracionDTO::new);
    }
    
}