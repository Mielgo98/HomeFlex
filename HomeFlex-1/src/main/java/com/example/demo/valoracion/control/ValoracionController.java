package com.example.demo.valoracion.control;

import java.security.Principal;
import java.util.List;

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

import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.service.PropiedadService;
import com.example.demo.reserva.model.EstadoReserva;
import com.example.demo.reserva.model.ReservaDTO;
import com.example.demo.reserva.service.ReservaService;
import com.example.demo.valoracion.model.NuevaValoracionDTO;
import com.example.demo.valoracion.model.RespuestaValoracionDTO;
import com.example.demo.valoracion.model.ValoracionDTO;
import com.example.demo.valoracion.model.ValoracionesEstadisticasDTO;
import com.example.demo.valoracion.service.ValoracionService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/valoraciones")
public class ValoracionController {

    @Autowired
    private ValoracionService valoracionService;
    
    @Autowired
    private PropiedadService propiedadService;
    
    @Autowired
    private ReservaService reservaService;
    
    /**
     * Muestra las valoraciones de una propiedad
     */
    @GetMapping("/propiedad/{propiedadId}")
    public String mostrarValoracionesPropiedad(
            @PathVariable Long propiedadId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        
        try {
            // Obtener la propiedad
            PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(propiedadId);
            model.addAttribute("propiedad", propiedad);
            
            // Obtener estadísticas de valoraciones
            ValoracionesEstadisticasDTO estadisticas = valoracionService.calcularEstadisticasPropiedad(propiedadId);
            model.addAttribute("estadisticas", estadisticas);
            
            // Obtener valoraciones paginadas
            Pageable pageable = PageRequest.of(pagina, size, Sort.by("fechaCreacion").descending());
            Page<ValoracionDTO> valoraciones = valoracionService.obtenerValoracionesPorPropiedadPaginadas(propiedadId, pageable);
            
            model.addAttribute("valoraciones", valoraciones);
            model.addAttribute("paginaActual", pagina);
            model.addAttribute("totalPaginas", valoraciones.getTotalPages());
            
            return "valoracion/valoraciones-propiedad";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar las valoraciones: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Muestra las valoraciones creadas por el usuario actual
     */
    @GetMapping("/mis-valoraciones")
    public String mostrarMisValoraciones(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            List<ValoracionDTO> valoraciones = valoracionService.obtenerValoracionesPorUsuario(principal.getName());
            model.addAttribute("valoraciones", valoraciones);
            
            return "valoracion/mis-valoraciones";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar tus valoraciones: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Muestra las valoraciones de las propiedades del propietario actual
     */
    @GetMapping("/mis-propiedades")
    public String mostrarValoracionesMisPropiedades(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int size,
            Model model,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            // Obtener valoraciones paginadas
            Pageable pageable = PageRequest.of(pagina, size, Sort.by("fechaCreacion").descending());
            Page<ValoracionDTO> valoraciones = valoracionService.obtenerValoracionesPorPropietarioPaginadas(
                    principal.getName(), pageable);
            
            // Obtener valoraciones pendientes de respuesta
            List<ValoracionDTO> pendientesRespuesta = valoracionService.obtenerValoracionesPendientesRespuesta(principal.getName());
            
            model.addAttribute("valoraciones", valoraciones);
            model.addAttribute("pendientesRespuesta", pendientesRespuesta);
            model.addAttribute("paginaActual", pagina);
            model.addAttribute("totalPaginas", valoraciones.getTotalPages());
            
            return "valoracion/valoraciones-propietario";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar las valoraciones: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Muestra el formulario para crear una nueva valoración
     */
    @GetMapping("/nueva/{propiedadId}")
    public String formularioNuevaValoracion(
            @PathVariable Long propiedadId,
            @RequestParam(required = false) Long reservaId,
            Model model,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            // Verificar que la propiedad existe
            PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(propiedadId);
            model.addAttribute("propiedad", propiedad);
            
            // Verificar si el usuario ya ha valorado esta propiedad
            try {
                List<ValoracionDTO> valoracionesUsuario = valoracionService.obtenerValoracionesPorUsuario(principal.getName());
                boolean yaValorada = valoracionesUsuario.stream()
                        .anyMatch(v -> v.getPropiedadId().equals(propiedadId));
                
                if (yaValorada) {
                    model.addAttribute("error", "Ya has valorado esta propiedad anteriormente");
                    return "error";
                }
            } catch (Exception e) {
                // Si ocurre un error, continuamos asumiendo que no hay valoración previa
            }
            
            // Si se proporciona un ID de reserva, verificar que existe y pertenece al usuario
            if (reservaId != null) {
                try {
                    ReservaDTO reserva = reservaService.obtenerReservaPorId(reservaId);
                    
                    // Verificar que la reserva pertenece al usuario y a la propiedad
                    if (!reserva.getNombreUsuario().equals(principal.getName()) || 
                        !reserva.getPropiedadId().equals(propiedadId)) {
                        model.addAttribute("error", "La reserva especificada no es válida para esta valoración");
                        return "error";
                    }
                    
                    // Verificar que la reserva está confirmada (completada)
                    if (reserva.getEstado() != EstadoReserva.CONFIRMADA) {
                        model.addAttribute("error", "Solo puedes valorar propiedades con reservas confirmadas");
                        return "error";
                    }
                    
                    model.addAttribute("reserva", reserva);
                } catch (Exception e) {
                    // Si ocurre un error, continuamos sin reserva
                }
            }
            
            // Crear DTO vacío para el formulario
            NuevaValoracionDTO valoracion = new NuevaValoracionDTO();
            valoracion.setPropiedadId(propiedadId);
            valoracion.setReservaId(reservaId);
            
            model.addAttribute("valoracion", valoracion);
            
            return "valoracion/formulario-valoracion";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar el formulario: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Procesa la creación de una nueva valoración
     */
    @PostMapping("/nueva")
    public String procesarNuevaValoracion(
            @Valid @ModelAttribute("valoracion") NuevaValoracionDTO valoracion,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        if (bindingResult.hasErrors()) {
            try {
                PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(valoracion.getPropiedadId());
                model.addAttribute("propiedad", propiedad);
                
                if (valoracion.getReservaId() != null) {
                    ReservaDTO reserva = reservaService.obtenerReservaPorId(valoracion.getReservaId());
                    model.addAttribute("reserva", reserva);
                }
                
                return "valoracion/formulario-valoracion";
            } catch (Exception e) {
                model.addAttribute("error", "Error al procesar el formulario: " + e.getMessage());
                return "error";
            }
        }
        
        try {
            ValoracionDTO nuevaValoracion = valoracionService.crearValoracion(valoracion, principal.getName());
            
            redirectAttributes.addFlashAttribute("mensaje", "Valoración enviada con éxito. ¡Gracias por compartir tu experiencia!");
            return "redirect:/valoraciones/mis-valoraciones";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al enviar la valoración: " + e.getMessage());
            return "redirect:/valoraciones/nueva/" + valoracion.getPropiedadId();
        }
    }
    
    /**
     * Muestra el formulario para editar una valoración existente
     */
    @GetMapping("/editar/{id}")
    public String formularioEditarValoracion(
            @PathVariable Long id,
            Model model,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            ValoracionDTO valoracion = valoracionService.obtenerValoracionPorId(id);
            
            // Verificar que el usuario es el autor de la valoración
            if (!valoracion.getUsuarioNombre().equals(principal.getName())) {
                model.addAttribute("error", "No tienes permiso para editar esta valoración");
                return "error";
            }
            
            // Obtener la propiedad
            PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(valoracion.getPropiedadId());
            model.addAttribute("propiedad", propiedad);
            
            // Convertir a DTO para edición
            NuevaValoracionDTO valoracionEdit = new NuevaValoracionDTO();
            valoracionEdit.setPropiedadId(valoracion.getPropiedadId());
            valoracionEdit.setPuntuacion(valoracion.getPuntuacion());
            valoracionEdit.setComentario(valoracion.getComentario());
            valoracionEdit.setLimpieza(valoracion.getLimpieza());
            valoracionEdit.setUbicacion(valoracion.getUbicacion());
            valoracionEdit.setComunicacion(valoracion.getComunicacion());
            valoracionEdit.setCalidad(valoracion.getCalidad());
            
            model.addAttribute("valoracion", valoracionEdit);
            model.addAttribute("valoracionId", id);
            model.addAttribute("esEdicion", true);
            
            return "valoracion/formulario-valoracion";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar la valoración: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Procesa la actualización de una valoración existente
     */
    @PostMapping("/editar/{id}")
    public String procesarEditarValoracion(
            @PathVariable Long id,
            @Valid @ModelAttribute("valoracion") NuevaValoracionDTO valoracion,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        if (bindingResult.hasErrors()) {
            try {
                PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(valoracion.getPropiedadId());
                model.addAttribute("propiedad", propiedad);
                model.addAttribute("valoracionId", id);
                model.addAttribute("esEdicion", true);
                
                return "valoracion/formulario-valoracion";
            } catch (Exception e) {
                model.addAttribute("error", "Error al procesar el formulario: " + e.getMessage());
                return "error";
            }
        }
        
        try {
            ValoracionDTO valoracionActualizada = valoracionService.actualizarValoracion(id, valoracion, principal.getName());
            
            redirectAttributes.addFlashAttribute("mensaje", "Valoración actualizada con éxito");
            return "redirect:/valoraciones/mis-valoraciones";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar la valoración: " + e.getMessage());
            return "redirect:/valoraciones/editar/" + id;
        }
    }
    
    /**
     * Elimina una valoración
     */
    @PostMapping("/eliminar/{id}")
    public String eliminarValoracion(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            valoracionService.eliminarValoracion(id, principal.getName());
            
            redirectAttributes.addFlashAttribute("mensaje", "Valoración eliminada con éxito");
            return "redirect:/valoraciones/mis-valoraciones";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la valoración: " + e.getMessage());
            return "redirect:/valoraciones/mis-valoraciones";
        }
    }
    
    /**
     * Muestra el formulario para responder a una valoración (como propietario)
     */
    @GetMapping("/responder/{id}")
    public String formularioResponderValoracion(
            @PathVariable Long id,
            Model model,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            ValoracionDTO valoracion = valoracionService.obtenerValoracionPorId(id);
            
            // Obtener la propiedad
            PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(valoracion.getPropiedadId());
            
            // Verificar que el usuario es el propietario de la propiedad
            if (!propiedad.getPropietario().getUsername().equals(principal.getName())) {
                model.addAttribute("error", "No tienes permiso para responder a esta valoración");
                return "error";
            }
            
            // Verificar que la valoración no tiene respuesta
            if (valoracion.getRespuestaPropietario() != null && !valoracion.getRespuestaPropietario().isEmpty()) {
                model.addAttribute("error", "Esta valoración ya tiene una respuesta");
                return "error";
            }
            
            model.addAttribute("valoracion", valoracion);
            model.addAttribute("propiedad", propiedad);
            
            // Crear DTO para la respuesta
            RespuestaValoracionDTO respuesta = new RespuestaValoracionDTO();
            respuesta.setValoracionId(id);
            
            model.addAttribute("respuesta", respuesta);
            
            return "valoracion/formulario-respuesta";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar la valoración: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Procesa la respuesta a una valoración
     */
    @PostMapping("/responder")
    public String procesarResponderValoracion(
            @Valid @ModelAttribute("respuesta") RespuestaValoracionDTO respuesta,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        if (bindingResult.hasErrors()) {
            try {
                ValoracionDTO valoracion = valoracionService.obtenerValoracionPorId(respuesta.getValoracionId());
                PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(valoracion.getPropiedadId());
                
                model.addAttribute("valoracion", valoracion);
                model.addAttribute("propiedad", propiedad);
                
                return "valoracion/formulario-respuesta";
            } catch (Exception e) {
                model.addAttribute("error", "Error al procesar el formulario: " + e.getMessage());
                return "error";
            }
        }
        
        try {
            valoracionService.responderValoracion(respuesta, principal.getName());
            
            redirectAttributes.addFlashAttribute("mensaje", "Respuesta enviada con éxito");
            return "redirect:/valoraciones/mis-propiedades";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al enviar la respuesta: " + e.getMessage());
            return "redirect:/valoraciones/responder/" + respuesta.getValoracionId();
        }
    }
    
    /**
     * Admin: Lista de valoraciones pendientes de aprobación
     */
    @GetMapping("/admin/pendientes")
    public String valoracionesPendientesAdmin(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            List<ValoracionDTO> valoracionesPendientes = valoracionService.obtenerValoracionesPendientes();
            model.addAttribute("valoraciones", valoracionesPendientes);
            
            return "valoracion/admin-valoraciones";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar las valoraciones pendientes: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Admin: Aprobar una valoración
     */
    @PostMapping("/admin/aprobar/{id}")
    public String aprobarValoracion(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            valoracionService.aprobarValoracion(id, principal.getName());
            
            redirectAttributes.addFlashAttribute("mensaje", "Valoración aprobada correctamente");
            return "redirect:/valoraciones/admin/pendientes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al aprobar la valoración: " + e.getMessage());
            return "redirect:/valoraciones/admin/pendientes";
        }
    }
    
    /**
     * Admin: Rechazar una valoración
     */
    @PostMapping("/admin/rechazar/{id}")
    public String rechazarValoracion(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            valoracionService.rechazarValoracion(id, principal.getName());
            
            redirectAttributes.addFlashAttribute("mensaje", "Valoración rechazada correctamente");
            return "redirect:/valoraciones/admin/pendientes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al rechazar la valoración: " + e.getMessage());
            return "redirect:/valoraciones/admin/pendientes";
        }
    }
}