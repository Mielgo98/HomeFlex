package com.example.demo.valoracion.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
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
import com.example.demo.valoracion.model.ValoracionesEstadisticasDTO;
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
    
    @Transactional
    public ValoracionDTO crearValoracion(NuevaValoracionDTO dto, String username) {
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(dto.getPropiedadId());
        
        if (dto.getReservaId() != null) {
            ReservaVO reserva = reservaService.obtenerReservaCompletaPorId(dto.getReservaId());
            if (!reserva.getUsuario().getId().equals(usuario.getId()) ||
                !reserva.getPropiedad().getId().equals(propiedad.getId())) {
                throw new RuntimeException("La reserva especificada no es válida para esta valoración");
            }
        }

        ValoracionVO valoracion = new ValoracionVO();
        valoracion.setPropiedad(propiedad);
        valoracion.setUsuario(usuario);
        valoracion.setPuntuacion(dto.getPuntuacion());
        valoracion.setComentario(dto.getComentario());
        valoracion.setFechaCreacion(LocalDateTime.now());
        valoracion.setAprobada(true);
        valoracion.setLimpieza(dto.getLimpieza());
        valoracion.setUbicacion(dto.getUbicacion());
        valoracion.setComunicacion(dto.getComunicacion());
        valoracion.setCalidad(dto.getCalidad());
        
        ValoracionVO guardada = valoracionRepository.save(valoracion);
        
        try {
            emailService.enviarNotificacionNuevaValoracion(
                propiedad.getPropietario().getEmail(),
                propiedad.getPropietario().getNombre(),
                usuario.getNombre(),
                propiedad.getTitulo(),
                guardada.getPuntuacion(),
                guardada.getComentario()
            );
        } catch (MessagingException e) {
            System.err.println("Error al enviar notificación de valoración: " + e.getMessage());
        }
        
        return new ValoracionDTO(guardada);
    }
    
    @Transactional
    public ValoracionDTO actualizarValoracion(Long id, NuevaValoracionDTO dto, String username) {
        ValoracionVO valoracion = valoracionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Valoración no encontrada"));
        
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        if (!valoracion.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("No tienes permiso para modificar esta valoración");
        }
        
        valoracion.setPuntuacion(dto.getPuntuacion());
        valoracion.setComentario(dto.getComentario());
        valoracion.setLimpieza(dto.getLimpieza());
        valoracion.setUbicacion(dto.getUbicacion());
        valoracion.setComunicacion(dto.getComunicacion());
        valoracion.setCalidad(dto.getCalidad());
        
        ValoracionVO actualizada = valoracionRepository.save(valoracion);
        return new ValoracionDTO(actualizada);
    }
    
    @Transactional
    public void eliminarValoracion(Long id, String username) {
        ValoracionVO valoracion = valoracionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Valoración no encontrada"));
        
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        boolean esAutor = valoracion.getUsuario().getId().equals(usuario.getId());
        boolean esAdmin = usuario.getRoles().stream()
                           .anyMatch(r -> r.getNombre().equals("ADMIN"));
        
        if (!esAutor && !esAdmin) {
            throw new RuntimeException("No tienes permiso para eliminar esta valoración");
        }
        
        valoracionRepository.delete(valoracion);
    }
    
    public ValoracionDTO obtenerValoracionPorId(Long id) {
        ValoracionVO v = valoracionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Valoración no encontrada"));
        return new ValoracionDTO(v);
    }
    
    public List<ValoracionDTO> obtenerValoracionesPorPropiedad(Long propiedadId) {
        PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(propiedadId);
        return valoracionRepository.findByPropiedadAndAprobadaTrue(propiedad)
                .stream()
                .map(ValoracionDTO::new)
                .collect(Collectors.toList());
    }
    
    public Page<ValoracionDTO> obtenerValoracionesPorPropiedadPaginadas(Long propiedadId, Pageable pageable) {
        PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(propiedadId);
        return valoracionRepository
                .findByPropiedadAndAprobadaTrue(propiedad, pageable)
                .map(ValoracionDTO::new);
    }
    
    public List<ValoracionDTO> obtenerValoracionesPorUsuario(String username) {
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        return valoracionRepository.findByUsuario(usuario)
                .stream()
                .map(ValoracionDTO::new)
                .collect(Collectors.toList());
    }
    
    public List<ValoracionDTO> obtenerValoracionesPorPropietario(String username) {
        UsuarioVO propietario = usuarioService.buscarPorUsername(username);
        return valoracionRepository.findByPropietario(propietario)
                .stream()
                .map(ValoracionDTO::new)
                .collect(Collectors.toList());
    }
    
    public Page<ValoracionDTO> obtenerValoracionesPorPropietarioPaginadas(String username, Pageable pageable) {
        UsuarioVO propietario = usuarioService.buscarPorUsername(username);
        return valoracionRepository
                .findByPropietario(propietario, pageable)
                .map(ValoracionDTO::new);
    }
    
    public List<ValoracionDTO> obtenerValoracionesPendientes() {
        return valoracionRepository.findAll().stream()
                .filter(v -> !v.getAprobada())
                .map(ValoracionDTO::new)
                .collect(Collectors.toList());
    }
    
    @Transactional
    public ValoracionDTO aprobarValoracion(Long id, String username) {
        UsuarioVO admin = usuarioService.buscarPorUsername(username);
        boolean esAdmin = admin.getRoles().stream()
                               .anyMatch(r -> r.getNombre().equals("ADMIN"));
        if (!esAdmin) {
            throw new RuntimeException("No tienes permisos para aprobar valoraciones");
        }
        
        ValoracionVO valoracion = valoracionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Valoración no encontrada"));
        valoracion.setAprobada(true);
        return new ValoracionDTO(valoracionRepository.save(valoracion));
    }
    
    @Transactional
    public void rechazarValoracion(Long id, String username) {
        UsuarioVO admin = usuarioService.buscarPorUsername(username);
        boolean esAdmin = admin.getRoles().stream()
                               .anyMatch(r -> r.getNombre().equals("ADMIN"));
        if (!esAdmin) {
            throw new RuntimeException("No tienes permisos para rechazar valoraciones");
        }
        
        ValoracionVO valoracion = valoracionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Valoración no encontrada"));
        valoracionRepository.delete(valoracion);
    }
    
    @Transactional
    public ValoracionDTO responderValoracion(RespuestaValoracionDTO dto, String username) {
        ValoracionVO valoracion = valoracionRepository.findById(dto.getValoracionId())
                .orElseThrow(() -> new RuntimeException("Valoración no encontrada"));
        
        UsuarioVO propietario = usuarioService.buscarPorUsername(username);
        if (!valoracion.getPropiedad().getPropietario().getId().equals(propietario.getId())) {
            throw new RuntimeException("No tienes permiso para responder a esta valoración");
        }
        
        valoracion.setRespuestaPropietario(dto.getRespuesta());
        valoracion.setFechaRespuesta(LocalDateTime.now());
        try {
            emailService.enviarNotificacionRespuestaValoracion(
                valoracion.getUsuario().getEmail(),
                valoracion.getUsuario().getNombre(),
                propietario.getNombre(),
                valoracion.getPropiedad().getTitulo(),
                dto.getRespuesta()
            );
        } catch (MessagingException e) {
            System.err.println("Error al enviar notificación de respuesta: " + e.getMessage());
        }
        return new ValoracionDTO(valoracionRepository.save(valoracion));
    }
    
    public Double calcularPuntuacionMediaPropiedad(Long propiedadId) {
        PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(propiedadId);
        Double media = valoracionRepository.calcularPuntuacionMediaPropiedad(propiedad);
        return media != null ? media : 0.0;
    }
    
    public ValoracionesEstadisticasDTO calcularEstadisticasPropiedad(Long propiedadId) {
        // CORRECCIÓN: usamos el método correcto para cargar la propiedad
        PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(propiedadId);
        
        ValoracionesEstadisticasDTO stats = new ValoracionesEstadisticasDTO();
        stats.setPropiedadId(propiedadId);
        stats.setPuntuacionGeneral(
            valoracionRepository.calcularPuntuacionMediaPropiedad(propiedad));
        stats.setPuntuacionLimpieza(
            valoracionRepository.calcularPuntuacionMediaLimpieza(propiedad));
        stats.setPuntuacionUbicacion(
            valoracionRepository.calcularPuntuacionMediaUbicacion(propiedad));
        stats.setPuntuacionComunicacion(
            valoracionRepository.calcularPuntuacionMediaComunicacion(propiedad));
        stats.setPuntuacionCalidad(
            valoracionRepository.calcularPuntuacionMediaCalidad(propiedad));
        stats.setNumeroValoraciones(
            (int) valoracionRepository.countByPropiedadAndAprobadaTrue(propiedad));
        return stats;
    }
    
    public List<ValoracionDTO> obtenerValoracionesPendientesRespuesta(String username) {
        UsuarioVO propietario = usuarioService.buscarPorUsername(username);
        return valoracionRepository.findByPropietario(propietario).stream()
                .filter(v -> v.getAprobada() && v.getRespuestaPropietario() == null)
                .map(ValoracionDTO::new)
                .collect(Collectors.toList());
    }
    
    public Page<ValoracionDTO> filtrarValoraciones(
            Long propiedadId,
            Boolean aprobada,
            Pageable pageable) {
        PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(propiedadId);
        return valoracionRepository.findByPropiedadAndAprobada(
                propiedad, aprobada, pageable)
            .map(ValoracionDTO::new);
    }
}
