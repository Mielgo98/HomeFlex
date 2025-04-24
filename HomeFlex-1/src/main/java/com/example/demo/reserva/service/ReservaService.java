package com.example.demo.reserva.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.service.PropiedadService;
import com.example.demo.reserva.model.EstadoReserva;
import com.example.demo.reserva.model.ReservaDTO;
import com.example.demo.reserva.model.ReservaVO;
import com.example.demo.reserva.model.SolicitudReservaDTO;
import com.example.demo.reserva.repository.ReservaRepository;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.service.UsuarioService;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepository;
    
    @Autowired
    private PropiedadService propiedadService;
    
    @Autowired
    private UsuarioService usuarioService;
    
    /**
     * Crea una nueva solicitud de reserva
     */
    @Transactional
    public ReservaDTO crearSolicitud(SolicitudReservaDTO solicitud, String username) {
        // Validar que el usuario existe
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        
        // Validar que la propiedad existe
        PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(solicitud.getPropiedadId());
        
        // Validar que el usuario no es el propietario
        if (propiedad.getPropietario().getUsername().equals(username)) {
            throw new RuntimeException("No puedes reservar tu propia propiedad");
        }
        
        // Validar fechas
        if (solicitud.getFechaInicio().isAfter(solicitud.getFechaFin())) {
            throw new RuntimeException("La fecha de inicio debe ser anterior a la fecha de fin");
        }
        
        if (solicitud.getFechaInicio().isBefore(LocalDate.now())) {
            throw new RuntimeException("La fecha de inicio debe ser futura");
        }
        
        // Validar número de huéspedes
        if (solicitud.getNumHuespedes() > propiedad.getCapacidad()) {
            throw new RuntimeException("El número de huéspedes excede la capacidad de la propiedad");
        }
        
        // Verificar disponibilidad
        if (reservaRepository.existsReservaActivaEnRango(propiedad, solicitud.getFechaInicio(), solicitud.getFechaFin())) {
            throw new RuntimeException("La propiedad no está disponible para las fechas seleccionadas");
        }
        
        // Calcular precio total
        BigDecimal precioTotal = calcularPrecioTotal(propiedad, solicitud.getFechaInicio(), solicitud.getFechaFin());
        
        // Crear la reserva
        ReservaVO reserva = new ReservaVO();
        reserva.setPropiedad(propiedad);
        reserva.setUsuario(usuario);
        reserva.setFechaInicio(solicitud.getFechaInicio());
        reserva.setFechaFin(solicitud.getFechaFin());
        reserva.setNumHuespedes(solicitud.getNumHuespedes());
        reserva.setPrecioTotal(precioTotal);
        reserva.setEstado(EstadoReserva.SOLICITADA);
        reserva.setFechaSolicitud(LocalDateTime.now());
        reserva.setCodigoReserva(generarCodigoReserva());
        reserva.setComentarios(solicitud.getComentarios());
        
        // Guardar la reserva
        ReservaVO reservaGuardada = reservaRepository.save(reserva);
        
        // Convertir a DTO
        return new ReservaDTO(reservaGuardada);
    }
    
    /**
     * Calcula el precio total de la reserva
     */
    private BigDecimal calcularPrecioTotal(PropiedadVO propiedad, LocalDate fechaInicio, LocalDate fechaFin) {
        long dias = ChronoUnit.DAYS.between(fechaInicio, fechaFin);
        
        // Si hay precio semanal y la estancia es de 7 días o más, se aplica ese precio
        if (propiedad.getPrecioSemana() != null && dias >= 7) {
            long semanas = dias / 7;
            long diasSueltos = dias % 7;
            
            BigDecimal precioPorSemanas = propiedad.getPrecioSemana().multiply(BigDecimal.valueOf(semanas));
            BigDecimal precioPorDias = propiedad.getPrecioDia().multiply(BigDecimal.valueOf(diasSueltos));
            
            return precioPorSemanas.add(precioPorDias);
        } else {
            // Precio por día
            return propiedad.getPrecioDia().multiply(BigDecimal.valueOf(dias));
        }
    }
    
    /**
     * Genera un código único para la reserva
     */
    private String generarCodigoReserva() {
        return "HF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    /**
     * Obtiene todas las reservas de un usuario (inquilino)
     */
    public List<ReservaDTO> obtenerReservasUsuario(String username) {
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        
        List<ReservaVO> reservas = reservaRepository.findByUsuario(usuario);
        
        return reservas.stream()
                .map(ReservaDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene todas las reservas de un usuario (inquilino) paginadas
     */
    public Page<ReservaDTO> obtenerReservasUsuarioPaginadas(String username, Pageable pageable) {
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        
        Page<ReservaVO> reservas = reservaRepository.findByUsuario(usuario, pageable);
        
        return reservas.map(ReservaDTO::new);
    }
    
    /**
     * Obtiene todas las reservas de las propiedades de un propietario
     */
    public List<ReservaDTO> obtenerReservasPropietario(String username) {
        UsuarioVO propietario = usuarioService.buscarPorUsername(username);
        
        List<ReservaVO> reservas = reservaRepository.findByPropietario(propietario);
        
        return reservas.stream()
                .map(ReservaDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene todas las reservas de las propiedades de un propietario paginadas
     */
    public Page<ReservaDTO> obtenerReservasPropietarioPaginadas(String username, Pageable pageable) {
        UsuarioVO propietario = usuarioService.buscarPorUsername(username);
        
        Page<ReservaVO> reservas = reservaRepository.findByPropietario(propietario, pageable);
        
        return reservas.map(ReservaDTO::new);
    }
    
    /**
     * Obtiene una reserva por su ID
     */
    public ReservaDTO obtenerReservaPorId(Long id) {
        ReservaVO reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        
        return new ReservaDTO(reserva);
    }
    
    /**
     * Obtiene una reserva completa por su ID
     */
    public ReservaVO obtenerReservaCompletaPorId(Long id) {
        return reservaRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
    }
    
    /**
     * Obtiene una reserva por su código
     */
    public ReservaDTO obtenerReservaPorCodigo(String codigo) {
        ReservaVO reserva = reservaRepository.findByCodigoReserva(codigo)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        
        return new ReservaDTO(reserva);
    }
    
    /**
     * Filtra reservas por estado para un usuario (inquilino)
     */
    public List<ReservaDTO> filtrarReservasUsuarioPorEstado(String username, EstadoReserva estado) {
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        
        List<ReservaVO> reservas = reservaRepository.findByUsuarioAndEstado(usuario, estado);
        
        return reservas.stream()
                .map(ReservaDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Filtra reservas por estado para un propietario
     */
    public List<ReservaDTO> filtrarReservasPropietarioPorEstado(String username, EstadoReserva estado) {
        UsuarioVO propietario = usuarioService.buscarPorUsername(username);
        
        List<ReservaVO> reservas = reservaRepository.findByPropietarioAndEstado(propietario, estado);
        
        return reservas.stream()
                .map(ReservaDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Aprueba una solicitud de reserva (cambia a estado PENDIENTE_PAGO)
     */
    @Transactional
    public ReservaDTO aprobarSolicitud(Long reservaId, String username) {
        ReservaVO reserva = obtenerReservaCompletaPorId(reservaId);
        
        // Verificar que el usuario es el propietario
        if (!reserva.getPropiedad().getPropietario().getUsername().equals(username)) {
            throw new RuntimeException("Solo el propietario puede aprobar la solicitud");
        }
        
        // Verificar que la reserva está en estado SOLICITADA
        if (reserva.getEstado() != EstadoReserva.SOLICITADA) {
            throw new RuntimeException("Solo se pueden aprobar solicitudes en estado SOLICITADA");
        }
        
        // Cambiar estado
        reserva.setEstado(EstadoReserva.PENDIENTE_PAGO);
        
        ReservaVO reservaActualizada = reservaRepository.save(reserva);
        
        return new ReservaDTO(reservaActualizada);
    }
    
    /**
     * Rechaza una solicitud de reserva (cambia a estado CANCELADA)
     */
    @Transactional
    public ReservaDTO rechazarSolicitud(Long reservaId, String username, String motivo) {
        ReservaVO reserva = obtenerReservaCompletaPorId(reservaId);
        
        // Verificar que el usuario es el propietario
        if (!reserva.getPropiedad().getPropietario().getUsername().equals(username)) {
            throw new RuntimeException("Solo el propietario puede rechazar la solicitud");
        }
        
        // Verificar que la reserva está en estado SOLICITADA
        if (reserva.getEstado() != EstadoReserva.SOLICITADA) {
            throw new RuntimeException("Solo se pueden rechazar solicitudes en estado SOLICITADA");
        }
        
        // Cambiar estado
        reserva.setEstado(EstadoReserva.CANCELADA);
        
        // Guardar motivo en los comentarios
        if (motivo != null && !motivo.trim().isEmpty()) {
            String comentariosActuales = reserva.getComentarios() != null ? reserva.getComentarios() : "";
            reserva.setComentarios(comentariosActuales + "\n\nMotivo de rechazo: " + motivo);
        }
        
        ReservaVO reservaActualizada = reservaRepository.save(reserva);
        
        return new ReservaDTO(reservaActualizada);
    }
    
    /**
     * Registra el pago de una reserva (cambia a estado PAGO_VERIFICADO)
     */
    @Transactional
    public ReservaDTO registrarPago(Long reservaId, String username) {
        ReservaVO reserva = obtenerReservaCompletaPorId(reservaId);
        
        // Verificar que el usuario es el inquilino
        if (!reserva.getUsuario().getUsername().equals(username)) {
            throw new RuntimeException("Solo el inquilino puede realizar el pago");
        }
        
        // Verificar que la reserva está en estado PENDIENTE_PAGO
        if (reserva.getEstado() != EstadoReserva.PENDIENTE_PAGO) {
            throw new RuntimeException("Solo se pueden pagar reservas en estado PENDIENTE_PAGO");
        }
        
        // Cambiar estado
        reserva.setEstado(EstadoReserva.PAGO_VERIFICADO);
        
        ReservaVO reservaActualizada = reservaRepository.save(reserva);
        
        return new ReservaDTO(reservaActualizada);
    }
    
    /**
     * Confirma una reserva (cambia a estado CONFIRMADA)
     */
    @Transactional
    public ReservaDTO confirmarReserva(Long reservaId, String username) {
        ReservaVO reserva = obtenerReservaCompletaPorId(reservaId);
        
        // Verificar que el usuario es el propietario
        if (!reserva.getPropiedad().getPropietario().getUsername().equals(username)) {
            throw new RuntimeException("Solo el propietario puede confirmar la reserva");
        }
        
        // Verificar que la reserva está en estado PAGO_VERIFICADO
        if (reserva.getEstado() != EstadoReserva.PAGO_VERIFICADO) {
            throw new RuntimeException("Solo se pueden confirmar reservas con pago verificado");
        }
        
        // Cambiar estado
        reserva.setEstado(EstadoReserva.CONFIRMADA);
        reserva.setFechaConfirmacion(LocalDateTime.now());
        
        ReservaVO reservaActualizada = reservaRepository.save(reserva);
        
        return new ReservaDTO(reservaActualizada);
    }
    
    /**
     * Cancela una reserva (cambia a estado CANCELADA)
     * Puede ser cancelada por el inquilino o el propietario
     */
    @Transactional
    public ReservaDTO cancelarReserva(Long reservaId, String username, String motivo) {
        ReservaVO reserva = obtenerReservaCompletaPorId(reservaId);
        
        // Verificar que el usuario es el inquilino o el propietario
        boolean esInquilino = reserva.getUsuario().getUsername().equals(username);
        boolean esPropietario = reserva.getPropiedad().getPropietario().getUsername().equals(username);
        
        if (!esInquilino && !esPropietario) {
            throw new RuntimeException("Solo el inquilino o el propietario pueden cancelar la reserva");
        }
        
        // Verificar que la reserva está en un estado cancelable
        if (reserva.getEstado() == EstadoReserva.CANCELADA) {
            throw new RuntimeException("La reserva ya está cancelada");
        }
        
        // Cambiar estado
        reserva.setEstado(EstadoReserva.CANCELADA);
        
        // Guardar motivo y quien canceló en los comentarios
        if (motivo != null && !motivo.trim().isEmpty()) {
            String comentariosActuales = reserva.getComentarios() != null ? reserva.getComentarios() : "";
            String quienCancela = esInquilino ? "inquilino" : "propietario";
            reserva.setComentarios(comentariosActuales + 
                    "\n\nReserva cancelada por " + quienCancela + ": " + motivo);
        }
        
        ReservaVO reservaActualizada = reservaRepository.save(reserva);
        
        return new ReservaDTO(reservaActualizada);
    }
    
    /**
     * Verifica la disponibilidad de una propiedad para un rango de fechas
     */
    public boolean verificarDisponibilidad(Long propiedadId, LocalDate fechaInicio, LocalDate fechaFin) {
        // Validar fechas
        if (fechaInicio.isAfter(fechaFin)) {
            throw new RuntimeException("La fecha de inicio debe ser anterior a la fecha de fin");
        }
        
        if (fechaInicio.isBefore(LocalDate.now())) {
            throw new RuntimeException("La fecha de inicio debe ser futura");
        }
        
        // Obtener la propiedad
        PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(propiedadId);
        
        // Verificar si hay reservas para ese rango
        return !reservaRepository.existsReservaActivaEnRango(propiedad, fechaInicio, fechaFin);
    }
    
    /**
     * Obtiene las reservas para una propiedad
     */
    public List<ReservaDTO> obtenerReservasPropiedad(Long propiedadId) {
        PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(propiedadId);
        
        List<ReservaVO> reservas = reservaRepository.findByPropiedad(propiedad);
        
        return reservas.stream()
                .map(ReservaDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene las fechas ocupadas para una propiedad en un rango (para mostrar en calendario)
     */
    public List<LocalDate> obtenerFechasOcupadas(Long propiedadId, LocalDate desde, LocalDate hasta) {
        PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(propiedadId);
        
        // Obtener las reservas activas en ese rango
        List<ReservaVO> reservasEnRango = reservaRepository.findReservasActivasEnRango(
                propiedad, desde, hasta);
        
        // Generar lista de todas las fechas ocupadas
        return reservasEnRango.stream()
                .flatMap(reserva -> {
                    // Para cada reserva, generar todas las fechas incluidas
                    LocalDate start = reserva.getFechaInicio();
                    LocalDate end = reserva.getFechaFin();
                    
                    return start.datesUntil(end.plusDays(1));
                })
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * Filtra reservas por criterios avanzados para un propietario
     */
    public Page<ReservaDTO> filtrarReservasPropietario(
            String username, 
            EstadoReserva estado, 
            String busqueda,
            Pageable pageable) {
        
        UsuarioVO propietario = usuarioService.buscarPorUsername(username);
        
        // Obtener todas las reservas del propietario
        List<ReservaVO> todasReservas;
        
        if (estado != null) {
            todasReservas = reservaRepository.findByPropietarioAndEstado(propietario, estado);
        } else {
            todasReservas = reservaRepository.findByPropietario(propietario);
        }
        
        // Filtrar por texto de búsqueda
        List<ReservaVO> reservasFiltradas = todasReservas;
        
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            String busquedaLower = busqueda.toLowerCase();
            
            reservasFiltradas = todasReservas.stream()
                    .filter(r -> 
                        r.getPropiedad().getTitulo().toLowerCase().contains(busquedaLower) ||
                        r.getUsuario().getNombre().toLowerCase().contains(busquedaLower) ||
                        r.getUsuario().getApellidos().toLowerCase().contains(busquedaLower) ||
                        r.getCodigoReserva().toLowerCase().contains(busquedaLower)
                    )
                    .collect(Collectors.toList());
        }
        
        // Aplicar paginación
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), reservasFiltradas.size());
        
        // Sublistado para la página actual
        List<ReservaVO> paginaReservas = start < end ? 
                reservasFiltradas.subList(start, end) : 
                List.of();
        
        // Convertir a DTOs
        List<ReservaDTO> reservasDTO = paginaReservas.stream()
                .map(ReservaDTO::new)
                .collect(Collectors.toList());
        
        return new PageImpl<>(reservasDTO, pageable, reservasFiltradas.size());
    }
    
    /**
     * Filtra reservas por criterios avanzados para un inquilino
     */
    public Page<ReservaDTO> filtrarReservasInquilino(
            String username, 
            EstadoReserva estado, 
            String busqueda,
            Pageable pageable) {
        
        UsuarioVO inquilino = usuarioService.buscarPorUsername(username);
        
        // Obtener todas las reservas del inquilino
        List<ReservaVO> todasReservas;
        
        if (estado != null) {
            todasReservas = reservaRepository.findByUsuarioAndEstado(inquilino, estado);
        } else {
            todasReservas = reservaRepository.findByUsuario(inquilino);
        }
        
        // Filtrar por texto de búsqueda
        List<ReservaVO> reservasFiltradas = todasReservas;
        
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            String busquedaLower = busqueda.toLowerCase();
            
            reservasFiltradas = todasReservas.stream()
                    .filter(r -> 
                        r.getPropiedad().getTitulo().toLowerCase().contains(busquedaLower) ||
                        r.getPropiedad().getCiudad().toLowerCase().contains(busquedaLower) ||
                        r.getPropiedad().getPais().toLowerCase().contains(busquedaLower) ||
                        r.getCodigoReserva().toLowerCase().contains(busquedaLower)
                    )
                    .collect(Collectors.toList());
        }
        
        // Aplicar paginación
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), reservasFiltradas.size());
        
        // Sublistado para la página actual
        List<ReservaVO> paginaReservas = start < end ? 
                reservasFiltradas.subList(start, end) : 
                List.of();
        
        // Convertir a DTOs
        List<ReservaDTO> reservasDTO = paginaReservas.stream()
                .map(ReservaDTO::new)
                .collect(Collectors.toList());
        
        return new PageImpl<>(reservasDTO, pageable, reservasFiltradas.size());
    }
}