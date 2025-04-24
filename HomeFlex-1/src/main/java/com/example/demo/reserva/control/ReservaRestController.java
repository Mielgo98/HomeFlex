package com.example.demo.reserva.control;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.reserva.model.EstadoReserva;
import com.example.demo.reserva.model.ReservaDTO;
import com.example.demo.reserva.model.SolicitudReservaDTO;
import com.example.demo.reserva.service.ReservaService;
import com.example.demo.utils.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservas")
public class ReservaRestController {

    @Autowired
    private ReservaService reservaService;
    
    /**
     * Obtiene las reservas del usuario autenticado
     */
    @GetMapping("/mis-reservas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ReservaDTO>> obtenerMisReservas(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda,
            Principal principal) {
        
        // Configurar paginación
        Pageable pageable = PageRequest.of(pagina, size, Sort.by("fechaInicio").descending());
        
        // Convertir estado string a enum si está presente
        EstadoReserva estadoEnum = null;
        if (estado != null && !estado.isEmpty()) {
            try {
                estadoEnum = EstadoReserva.valueOf(estado);
            } catch (IllegalArgumentException e) {
                // Ignorar estado inválido
            }
        }
        
        // Obtener reservas filtradas
        Page<ReservaDTO> reservas = reservaService.filtrarReservasInquilino(
                principal.getName(), estadoEnum, busqueda, pageable);
        
        return ResponseEntity.ok(reservas);
    }
    
    /**
     * Obtiene las solicitudes de reserva para las propiedades del propietario
     */
    @GetMapping("/solicitudes")
    @PreAuthorize("hasRole('ROLE_PROPIETARIO')")
    public ResponseEntity<Page<ReservaDTO>> obtenerSolicitudes(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda,
            Principal principal) {
        
        // Configurar paginación
        Pageable pageable = PageRequest.of(pagina, size, Sort.by("fechaSolicitud").descending());
        
        // Convertir estado string a enum si está presente
        EstadoReserva estadoEnum = null;
        if (estado != null && !estado.isEmpty()) {
            try {
                estadoEnum = EstadoReserva.valueOf(estado);
            } catch (IllegalArgumentException e) {
                // Ignorar estado inválido
            }
        }
        
        // Obtener reservas filtradas
        Page<ReservaDTO> reservas = reservaService.filtrarReservasPropietario(
                principal.getName(), estadoEnum, busqueda, pageable);
        
        return ResponseEntity.ok(reservas);
    }
    
    /**
     * Obtiene una reserva por su ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerReserva(@PathVariable Long id, Principal principal) {
        try {
            ReservaDTO reserva = reservaService.obtenerReservaPorId(id);
            
            // Verificar que el usuario es el inquilino o el propietario
            boolean esInquilino = reserva.getNombreUsuario().equals(principal.getName());
            boolean esPropietario = reserva.getTituloPropiedad().equals(principal.getName());
            
            if (!esInquilino && !esPropietario) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "No tienes permiso para ver esta reserva"));
            }
            
            return ResponseEntity.ok(reserva);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Error al cargar la reserva: " + e.getMessage()));
        }
    }
    
    /**
     * Crea una nueva solicitud de reserva
     */
    @PostMapping("/solicitar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> solicitarReserva(
            @Valid @RequestBody SolicitudReservaDTO solicitud,
            Principal principal) {
        
        try {
            // Verificar disponibilidad
            boolean disponible = reservaService.verificarDisponibilidad(
                    solicitud.getPropiedadId(), solicitud.getFechaInicio(), solicitud.getFechaFin());
            
            if (!disponible) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponse(false, "La propiedad no está disponible para las fechas seleccionadas"));
            }
            
            // Crear la solicitud
            ReservaDTO reservaCreada = reservaService.crearSolicitud(solicitud, principal.getName());
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Solicitud de reserva creada con éxito", reservaCreada));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al solicitar la reserva: " + e.getMessage()));
        }
    }
    
    /**
     * Verifica la disponibilidad de una propiedad para un rango de fechas
     */
    @GetMapping("/verificar-disponibilidad")
    public ResponseEntity<?> verificarDisponibilidad(
            @RequestParam Long propiedadId,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin) {
        
        try {
            LocalDate inicio = LocalDate.parse(fechaInicio);
            LocalDate fin = LocalDate.parse(fechaFin);
            
            boolean disponible = reservaService.verificarDisponibilidad(propiedadId, inicio, fin);
            
            return ResponseEntity.ok(new ApiResponse(true, "Verificación completada", disponible));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al verificar disponibilidad: " + e.getMessage()));
        }
    }
    
    /**
     * Obtiene las fechas ocupadas para una propiedad (para el calendario)
     */
    @GetMapping("/fechas-ocupadas/{propiedadId}")
    public ResponseEntity<?> obtenerFechasOcupadas(
            @PathVariable Long propiedadId,
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        
        try {
            LocalDate fechaDesde = desde != null ? LocalDate.parse(desde) : LocalDate.now();
            LocalDate fechaHasta = hasta != null ? LocalDate.parse(hasta) : fechaDesde.plusMonths(3);
            
            List<LocalDate> fechasOcupadas = reservaService.obtenerFechasOcupadas(
                    propiedadId, fechaDesde, fechaHasta);
            
            return ResponseEntity.ok(fechasOcupadas);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al obtener fechas ocupadas: " + e.getMessage()));
        }
    }
    
    /**
     * Aprueba una solicitud de reserva
     */
    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ROLE_PROPIETARIO')")
    public ResponseEntity<?> aprobarSolicitud(@PathVariable Long id, Principal principal) {
        try {
            ReservaDTO reserva = reservaService.aprobarSolicitud(id, principal.getName());
            
            return ResponseEntity.ok(new ApiResponse(true, "Solicitud aprobada correctamente", reserva));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al aprobar la solicitud: " + e.getMessage()));
        }
    }
    
    /**
     * Rechaza una solicitud de reserva
     */
    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ROLE_PROPIETARIO')")
    public ResponseEntity<?> rechazarSolicitud(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo,
            Principal principal) {
        
        try {
            ReservaDTO reserva = reservaService.rechazarSolicitud(id, principal.getName(), motivo);
            
            return ResponseEntity.ok(new ApiResponse(true, "Solicitud rechazada correctamente", reserva));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al rechazar la solicitud: " + e.getMessage()));
        }
    }
    
    /**
     * Registra el pago de una reserva
     */
    @PostMapping("/{id}/pagar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> registrarPago(@PathVariable Long id, Principal principal) {
        try {
            ReservaDTO reserva = reservaService.registrarPago(id, principal.getName());
            
            return ResponseEntity.ok(new ApiResponse(true, "Pago registrado correctamente", reserva));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al registrar el pago: " + e.getMessage()));
        }
    }
    
    /**
     * Confirma una reserva
     */
    @PostMapping("/{id}/confirmar")
    @PreAuthorize("hasRole('ROLE_PROPIETARIO')")
    public ResponseEntity<?> confirmarReserva(@PathVariable Long id, Principal principal) {
        try {
            ReservaDTO reserva = reservaService.confirmarReserva(id, principal.getName());
            
            return ResponseEntity.ok(new ApiResponse(true, "Reserva confirmada correctamente", reserva));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al confirmar la reserva: " + e.getMessage()));
        }
    }
    
    /**
     * Cancela una reserva
     */
    @PostMapping("/{id}/cancelar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> cancelarReserva(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo,
            Principal principal) {
        
        try {
            ReservaDTO reserva = reservaService.cancelarReserva(id, principal.getName(), motivo);
            
            return ResponseEntity.ok(new ApiResponse(true, "Reserva cancelada correctamente", reserva));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al cancelar la reserva: " + e.getMessage()));
        }
    }
}