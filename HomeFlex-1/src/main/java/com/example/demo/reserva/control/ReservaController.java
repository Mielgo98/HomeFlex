package com.example.demo.reserva.control;

import java.security.Principal;
import java.time.LocalDate;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.propiedad.service.PropiedadService;
import com.example.demo.reserva.model.EstadoReserva;
import com.example.demo.reserva.model.ReservaDTO;
import com.example.demo.reserva.model.SolicitudReservaDTO;
import com.example.demo.reserva.service.ReservaService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/reservas")
public class ReservaController {

    @Autowired
    private ReservaService reservaService;
    
    @Autowired
    private PropiedadService propiedadService;
    
    /**
     * Muestra las reservas del usuario (inquilino)
     */
    @GetMapping("/mis-reservas")
    public String mostrarMisReservas(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda,
            Model model,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
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
        
        model.addAttribute("reservas", reservas);
        model.addAttribute("paginaActual", pagina);
        model.addAttribute("totalPaginas", reservas.getTotalPages());
        model.addAttribute("estado", estado);
        model.addAttribute("busqueda", busqueda);
        
        return "reserva/mis-reservas";
    }
    
    /**
     * Muestra las reservas de las propiedades del propietario
     */
    @GetMapping("/solicitudes")
    public String mostrarSolicitudes(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String busqueda,
            Model model,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
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
        
        model.addAttribute("reservas", reservas);
        model.addAttribute("paginaActual", pagina);
        model.addAttribute("totalPaginas", reservas.getTotalPages());
        model.addAttribute("estado", estado);
        model.addAttribute("busqueda", busqueda);
        
        return "reserva/solicitudes";
    }
    
    /**
     * Muestra el detalle de una reserva
     */
    @GetMapping("/{id}")
    public String verReserva(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            ReservaDTO reserva = reservaService.obtenerReservaPorId(id);
            
            // Verificar que el usuario es el inquilino o el propietario
            boolean esInquilino = reserva.getNombreUsuario().equals(principal.getName());
            boolean esPropietario = reserva.getTituloPropiedad().equals(principal.getName());
            
            if (!esInquilino && !esPropietario) {
                model.addAttribute("error", "No tienes permiso para ver esta reserva");
                return "error";
            }
            
            model.addAttribute("reserva", reserva);
            model.addAttribute("esInquilino", esInquilino);
            model.addAttribute("esPropietario", esPropietario);
            
            return "reserva/detalle";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar la reserva: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Procesa la solicitud de una nueva reserva
     */
    @PostMapping("/solicitar")
    public String solicitarReserva(
            @Valid @ModelAttribute("solicitudReserva") SolicitudReservaDTO solicitud,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Por favor, complete correctamente todos los campos obligatorios");
            model.addAttribute("propiedadId", solicitud.getPropiedadId());
            return "propiedad/detalle";
        }
        
        try {
            // Verificar disponibilidad
            boolean disponible = reservaService.verificarDisponibilidad(
                    solicitud.getPropiedadId(), solicitud.getFechaInicio(), solicitud.getFechaFin());
            
            if (!disponible) {
                redirectAttributes.addFlashAttribute("error", 
                        "La propiedad no está disponible para las fechas seleccionadas");
                return "redirect:/propiedades/" + solicitud.getPropiedadId();
            }
            
            // Crear la solicitud
            ReservaDTO reservaCreada = reservaService.crearSolicitud(solicitud, principal.getName());
            
            redirectAttributes.addFlashAttribute("mensaje", 
                    "¡Solicitud de reserva enviada con éxito! Código: " + reservaCreada.getCodigoReserva());
            return "redirect:/reservas/mis-reservas";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al solicitar la reserva: " + e.getMessage());
            return "redirect:/propiedades/" + solicitud.getPropiedadId();
        }
    }
    
    /**
     * Aprueba una solicitud de reserva
     */
    @PostMapping("/{id}/aprobar")
    public String aprobarSolicitud(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            reservaService.aprobarSolicitud(id, principal.getName());
            
            redirectAttributes.addFlashAttribute("mensaje", "Solicitud aprobada correctamente");
            return "redirect:/reservas/solicitudes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al aprobar la solicitud: " + e.getMessage());
            return "redirect:/reservas/" + id;
        }
    }
    
    /**
     * Rechaza una solicitud de reserva
     */
    @PostMapping("/{id}/rechazar")
    public String rechazarSolicitud(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            reservaService.rechazarSolicitud(id, principal.getName(), motivo);
            
            redirectAttributes.addFlashAttribute("mensaje", "Solicitud rechazada correctamente");
            return "redirect:/reservas/solicitudes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al rechazar la solicitud: " + e.getMessage());
            return "redirect:/reservas/" + id;
        }
    }
    
    /**
     * Registra el pago de una reserva
     */
    @PostMapping("/{id}/pagar")
    public String registrarPago(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            reservaService.registrarPago(id, principal.getName());
            
            redirectAttributes.addFlashAttribute("mensaje", "Pago registrado correctamente");
            return "redirect:/reservas/mis-reservas";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al registrar el pago: " + e.getMessage());
            return "redirect:/reservas/" + id;
        }
    }
    
    /**
     * Confirma una reserva
     */
    @PostMapping("/{id}/confirmar")
    public String confirmarReserva(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            reservaService.confirmarReserva(id, principal.getName());
            
            redirectAttributes.addFlashAttribute("mensaje", "Reserva confirmada correctamente");
            return "redirect:/reservas/solicitudes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al confirmar la reserva: " + e.getMessage());
            return "redirect:/reservas/" + id;
        }
    }
    
    /**
     * Cancela una reserva
     */
    @PostMapping("/{id}/cancelar")
    public String cancelarReserva(
            @PathVariable Long id,
            @RequestParam(required = false) String motivo,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            reservaService.cancelarReserva(id, principal.getName(), motivo);
            
            redirectAttributes.addFlashAttribute("mensaje", "Reserva cancelada correctamente");
            
            // Redireccionar según el rol
            ReservaDTO reserva = reservaService.obtenerReservaPorId(id);
            boolean esInquilino = reserva.getNombreUsuario().equals(principal.getName());
            
            if (esInquilino) {
                return "redirect:/reservas/mis-reservas";
            } else {
                return "redirect:/reservas/solicitudes";
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cancelar la reserva: " + e.getMessage());
            return "redirect:/reservas/" + id;
        }
    }
    
    /**
     * Verificar disponibilidad (AJAX)
     */
    @GetMapping("/verificar-disponibilidad")
    public String verificarDisponibilidad(
            @RequestParam Long propiedadId,
            @RequestParam String fechaInicio,
            @RequestParam String fechaFin,
            Model model) {
        
        try {
            LocalDate inicio = LocalDate.parse(fechaInicio);
            LocalDate fin = LocalDate.parse(fechaFin);
            
            boolean disponible = reservaService.verificarDisponibilidad(propiedadId, inicio, fin);
            
            model.addAttribute("disponible", disponible);
            
            return "reserva/fragmento-disponibilidad";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("disponible", false);
            
            return "reserva/fragmento-disponibilidad";
        }
    }
}