package com.example.demo.pago.control;

import java.security.Principal;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.pago.model.EstadoPago;
import com.example.demo.pago.model.PagoDTO;
import com.example.demo.pago.model.PagoVO;
import com.example.demo.pago.model.PagoResponse;
import com.example.demo.pago.service.PagoGestionService;
import com.example.demo.pago.service.PagoService;
import com.example.demo.reserva.model.ReservaDTO;
import com.example.demo.reserva.service.ReservaService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/pago")
public class PagoController {

    @Autowired
    private PagoService pagoService;
    
    @Autowired
    private PagoGestionService pagoGestionService;
    
    @Autowired
    private ReservaService reservaService;
    
    /**
     * Inicia el proceso de pago para una reserva
     */
    @GetMapping("/iniciar")
    public String iniciarPago(
            @RequestParam Long reservaId,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            // Iniciar el proceso de pago
            String urlPago = pagoGestionService.iniciarProcesoDeCompra(reservaId, principal.getName());
            
            // Redirigir a la página de pago
            return "redirect:" + urlPago;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al iniciar el proceso de pago: " + e.getMessage());
            return "redirect:/reservas/" + reservaId;
        }
    }
    
    /**
     * Muestra el formulario de pago
     */
    @GetMapping("/formulario")
    public String mostrarFormularioPago(
            @RequestParam Long reservaId, 
            @RequestParam String idSesion,
            Model model,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            // Verificar que la reserva existe y pertenece al usuario
            ReservaDTO reserva = reservaService.obtenerReservaPorId(reservaId);
            
            // Verificar que el usuario es el inquilino de la reserva
            if (!reserva.getNombreUsuario().equals(principal.getName())) {
                model.addAttribute("error", "No tienes permiso para realizar este pago");
                return "error";
            }
            
            // Preparar DTO para el formulario
            PagoDTO pagoDTO = new PagoDTO();
            pagoDTO.setReservaId(reservaId);
            pagoDTO.setMonto(reserva.getPrecioTotal());
            pagoDTO.setIdSesion(idSesion);
            
            model.addAttribute("pago", pagoDTO);
            model.addAttribute("reserva", reserva);
            
            return "pago/formulario-pago";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar el formulario de pago: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Procesa el pago
     */
    @PostMapping("/procesar")
    public String procesarPago(
            @Valid @ModelAttribute("pago") PagoDTO pagoDTO,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        if (bindingResult.hasErrors()) {
            try {
                ReservaDTO reserva = reservaService.obtenerReservaPorId(pagoDTO.getReservaId());
                model.addAttribute("reserva", reserva);
                
                return "pago/formulario-pago";
            } catch (Exception e) {
                model.addAttribute("error", "Error al procesar el formulario: " + e.getMessage());
                return "error";
            }
        }
        
        try {
            // Procesar el pago
            PagoResponse respuesta = pagoGestionService.procesarPago(pagoDTO, principal.getName());
            
            if ("success".equals(respuesta.getResultado())) {
                redirectAttributes.addFlashAttribute("mensajeExito", 
                        "¡Pago realizado con éxito! Tu reserva ha sido confirmada.");
                return "redirect:/pago/exito?idSesion=" + pagoDTO.getIdSesion();
            } else {
                // Si el pago falló
                model.addAttribute("error", "Error en el pago: " + respuesta.getMensaje());
                model.addAttribute("codigoError", respuesta.getCodigoError());
                model.addAttribute("pago", pagoDTO);
                
                try {
                    ReservaDTO reserva = reservaService.obtenerReservaPorId(pagoDTO.getReservaId());
                    model.addAttribute("reserva", reserva);
                } catch (Exception e) {
                    // Ignorar error al cargar reserva
                }
                
                return "pago/formulario-pago";
            }
        } catch (Exception e) {
            model.addAttribute("error", "Error al procesar el pago: " + e.getMessage());
            
            try {
                ReservaDTO reserva = reservaService.obtenerReservaPorId(pagoDTO.getReservaId());
                model.addAttribute("reserva", reserva);
            } catch (Exception ex) {
                // Ignorar error al cargar reserva
            }
            
            return "pago/formulario-pago";
        }
    }
    
    /**
     * Simulador de pago (para pruebas)
     */
    @GetMapping("/simulador")
    public String simuladorPago(
            @RequestParam Long reservaId,
            @RequestParam(required = false) String idSesion,
            @RequestParam(required = false) String monto,
            Model model,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            // Obtener información de la reserva
            ReservaDTO reserva = reservaService.obtenerReservaPorId(reservaId);
            
            // Verificar que el usuario es el inquilino de la reserva
            if (!reserva.getNombreUsuario().equals(principal.getName())) {
                model.addAttribute("error", "No tienes permiso para realizar este pago");
                return "error";
            }
            
            // Preparar datos para el simulador
            PagoDTO pagoDTO = new PagoDTO();
            pagoDTO.setReservaId(reservaId);
            pagoDTO.setMonto(reserva.getPrecioTotal());
            pagoDTO.setIdSesion(idSesion != null ? idSesion : "sess_" + System.currentTimeMillis());
            
            model.addAttribute("pago", pagoDTO);
            model.addAttribute("reserva", reserva);
            model.addAttribute("esSimulador", true);
            
            return "pago/simulador-pago";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar el simulador de pago: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Webhook para recibir notificaciones del procesador de pagos
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> procesarWebhook(@RequestBody Map<String, Object> payload) {
        try {
            // Validar la firma del webhook (en un entorno real)
            // Extraer datos relevantes
            String idSesion = (String) payload.get("id");
            String estado = (String) payload.get("status");
            
            // Buscar el pago asociado
            PagoVO pago = pagoGestionService.obtenerPagoPorIdSesion(idSesion);
            
            // Actualizar el estado del pago
            if ("succeeded".equals(estado)) {
                pago.setEstado(EstadoPago.COMPLETADO);
            } else if ("failed".equals(estado)) {
                pago.setEstado(EstadoPago.FALLIDO);
            }
            
            // Verificar y actualizar el pago
            pagoGestionService.verificarYActualizarPago(idSesion);
            
            return ResponseEntity.ok("Webhook procesado correctamente");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error al procesar webhook: " + e.getMessage());
        }
    }
    
    /**
     * Página de éxito del pago
     */
    @GetMapping("/exito")
    public String pagoExitoso(
            @RequestParam String idSesion,
            Model model,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            // Verificar y actualizar el pago
            PagoVO pago = pagoGestionService.verificarYActualizarPago(idSesion);
            
            // Verificar que el pago pertenece al usuario
            if (!pago.getUsuario().getUsername().equals(principal.getName())) {
                model.addAttribute("error", "No tienes permiso para ver este pago");
                return "error";
            }
            
            // Obtener información de la reserva
            ReservaDTO reserva = reservaService.obtenerReservaPorId(pago.getReserva().getId());
            
            model.addAttribute("pago", pago);
            model.addAttribute("reserva", reserva);
            
            return "pago/pago-exitoso";
        } catch (Exception e) {
            model.addAttribute("error", "Error al verificar el pago: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Página de fallo del pago
     */
    @GetMapping("/fallido")
    public String pagoFallido(
            @RequestParam String idSesion,
            @RequestParam(required = false) String error,
            Model model,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            // Buscar el pago
            PagoVO pago = pagoGestionService.obtenerPagoPorIdSesion(idSesion);
            
            // Verificar que el pago pertenece al usuario
            if (!pago.getUsuario().getUsername().equals(principal.getName())) {
                model.addAttribute("error", "No tienes permiso para ver este pago");
                return "error";
            }
            
            // Obtener información de la reserva
            ReservaDTO reserva = reservaService.obtenerReservaPorId(pago.getReserva().getId());
            
            model.addAttribute("pago", pago);
            model.addAttribute("reserva", reserva);
            model.addAttribute("errorPago", error);
            
            return "pago/pago-fallido";
        } catch (Exception e) {
            model.addAttribute("error", "Error al verificar el pago: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Lista los pagos de una reserva
     */
    @GetMapping("/reserva/{reservaId}")
    public String listarPagosPorReserva(
            @PathVariable Long reservaId,
            Model model,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            // Verificar que la reserva existe
            ReservaDTO reserva = reservaService.obtenerReservaPorId(reservaId);
            
            // Verificar que el usuario es el inquilino o el propietario
            boolean esInquilino = reserva.getNombreUsuario().equals(principal.getName());
            
            if (!esInquilino) {
                model.addAttribute("error", "No tienes permiso para ver estos pagos");
                return "error";
            }
            
            // Obtener los pagos de la reserva
            List<PagoVO> pagos = pagoGestionService.obtenerPagosPorReserva(reservaId);
            
            model.addAttribute("pagos", pagos);
            model.addAttribute("reserva", reserva);
            
            return "pago/lista-pagos";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar los pagos: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Cancela un pago pendiente
     */
    @PostMapping("/cancelar/{pagoId}")
    public String cancelarPago(
            @PathVariable Long pagoId,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            // Cancelar el pago
            boolean cancelado = pagoGestionService.cancelarPago(pagoId, principal.getName());
            
            if (cancelado) {
                redirectAttributes.addFlashAttribute("mensajeExito", "Pago cancelado correctamente");
            } else {
                redirectAttributes.addFlashAttribute("error", "No se pudo cancelar el pago");
            }
            
            // Redirigir a la lista de reservas
            return "redirect:/reservas/mis-reservas";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cancelar el pago: " + e.getMessage());
            return "redirect:/reservas/mis-reservas";
        }
    }
    
    /**
     * Solicita un reembolso (para propietarios)
     */
    @PostMapping("/reembolsar/{pagoId}")
    public String reembolsarPago(
            @PathVariable Long pagoId,
            @RequestParam String motivo,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            // Reembolsar el pago
            boolean reembolsado = pagoGestionService.reembolsarPago(pagoId, motivo, principal.getName());
            
            if (reembolsado) {
                redirectAttributes.addFlashAttribute("mensajeExito", "Pago reembolsado correctamente");
            } else {
                redirectAttributes.addFlashAttribute("error", "No se pudo reembolsar el pago");
            }
            
            // Redirigir a la lista de reservas
            return "redirect:/reservas/solicitudes";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al reembolsar el pago: " + e.getMessage());
            return "redirect:/reservas/solicitudes";
        }
    }
}