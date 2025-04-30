package com.example.demo.reserva.control;

import java.security.Principal;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.pago.model.PagoDTO;
import com.example.demo.pago.service.PagoService;
import com.example.demo.propiedad.model.PropiedadDTO;
import com.example.demo.propiedad.service.PropiedadService;
import com.example.demo.reserva.model.ReservaDTO;
import com.example.demo.reserva.model.SolicitudReservaDTO;
import com.example.demo.reserva.service.ReservaService;

@Controller
@RequestMapping("/reservas")
public class ReservaController {

    @Autowired
    private PropiedadService propiedadService;

    @Autowired
    private ReservaService reservaService;

    @Autowired
    private PagoService pagoService;
    
    /**
     * Muestra el formulario para crear una nueva reserva de una propiedad dada
     */
    @GetMapping("/nueva/{propiedadId}")
    public String nuevaReservaForm(@PathVariable Long propiedadId,
                                   Model model,
                                   Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        // Cargar datos de la propiedad
        PropiedadDTO propiedad = propiedadService.obtenerPropiedadPorId(propiedadId);
        model.addAttribute("propiedad", propiedad);

        // DTO de solicitud vacío (bindear formulario)
        SolicitudReservaDTO solicitudDTO = new SolicitudReservaDTO();
        solicitudDTO.setPropiedadId(propiedadId);
        model.addAttribute("solicitudDTO", solicitudDTO);

        return "reserva/nueva-reserva";
    }

    /**
     * Procesa el envío del formulario de nueva reserva
     */
    @PostMapping("/nueva")
    public String crearReserva(@Valid @ModelAttribute("solicitudDTO") SolicitudReservaDTO solicitudDTO,
                               BindingResult bindingResult,
                               Principal principal,
                               RedirectAttributes ra,
                               Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        if (bindingResult.hasErrors()) {
            // Volver a mostrar formulario con errores; recargar propiedad
            PropiedadDTO propiedad = propiedadService
                    .obtenerPropiedadPorId(solicitudDTO.getPropiedadId());
            model.addAttribute("propiedad", propiedad);
            return "reserva/nueva-reserva";
        }

        try {
            // Llamamos al método que acepta SolicitudReservaDTO + username
            ReservaDTO creada = reservaService
                    .crearSolicitud(solicitudDTO, principal.getName());

            ra.addFlashAttribute("mensajeExito",
                    "Reserva creada correctamente (código: " + creada.getCodigoReserva() + ")");
            return "redirect:/reservas/mis-reservas";

        } catch (Exception e) {
            // En caso de error, redirigir al formulario con mensaje
            ra.addFlashAttribute("error", "Error al crear la reserva: " + e.getMessage());
            return "redirect:/reservas/nueva/" + solicitudDTO.getPropiedadId();
        }
    }
    /**
     * Muestra el detalle de una reserva concreta, junto con sus pagos.
     */
    @GetMapping("/{id}")
    public String detalleReserva(@PathVariable Long id,
                                 Model model,
                                 Authentication auth,
                                 RedirectAttributes ra) {
       

        // 2) Obtener la reserva validando que pertenece al usuario
		ReservaDTO reserva = reservaService.obtenerReservaPorId(id);
		model.addAttribute("reserva", reserva);

		List<PagoDTO> pagos = pagoService.obtenerPorReserva(id);
		model.addAttribute("pagos", pagos);

       // 4) Determinar roles
		boolean esInquilino   = auth.getAuthorities().stream()
		    .anyMatch(a -> a.getAuthority().equals("ROLE_INQUILINO"));
		boolean esPropietario = auth.getAuthorities().stream()
		    .anyMatch(a -> a.getAuthority().equals("ROLE_PROPIETARIO"));
		model.addAttribute("esInquilino",   esInquilino);
		model.addAttribute("esPropietario", esPropietario);

		// 5) Devolver la plantilla de detalle
		return "reserva/detalle";
    }

    
    /**
     * Lista las reservas del usuario
     */
    @GetMapping("/mis-reservas")
    public String listarMisReservas(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        model.addAttribute("reservas",
                reservaService.obtenerReservasUsuario(principal.getName()));
        return "reserva/mis-reservas";
    }
}
