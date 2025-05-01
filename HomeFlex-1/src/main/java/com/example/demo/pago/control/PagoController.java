package com.example.demo.pago.control;

import java.util.List;
import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.config.StripeConfig;
import com.example.demo.pago.model.PagoDTO;
import com.example.demo.pago.model.PagoVO;
import com.example.demo.pago.service.PagoGestionService;
import com.example.demo.pago.service.PagoService;
import com.example.demo.reserva.model.ReservaDTO;
import com.example.demo.reserva.service.ReservaService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/pago")
public class PagoController {

    private final PagoService pagoService;
    private final PagoGestionService pagoGestionService;
    private final ReservaService reservaService;
    private final StripeConfig stripeConfig;

    public PagoController(PagoService pagoService,
                          PagoGestionService pagoGestionService,
                          ReservaService reservaService,
                          StripeConfig stripeConfig) {
        this.pagoService = pagoService;
        this.pagoGestionService = pagoGestionService;
        this.reservaService = reservaService;
        this.stripeConfig = stripeConfig;
    }

    /**
     * Muestra el formulario de pago para una reserva
     */
    @GetMapping("/formulario")
    public String mostrarFormularioPago(@RequestParam Long reservaId,
                                       @RequestParam(required = false) String idSesion,
                                       Model model,
                                       Principal principal) {
        if (principal == null) return "redirect:/login";
        
        // Obtener la reserva
        ReservaDTO reserva = reservaService.obtenerReservaPorId(reservaId);
        
        // Verificar que el usuario es el inquilino
        if (!reserva.getNombreUsuario().equals(principal.getName())) {
            model.addAttribute("error", "No tienes permiso para realizar este pago");
            return "error";
        }
        
        // Crear o recuperar el pago
        PagoVO pagoVO;
        PagoDTO pagoDTO = new PagoDTO();
        
        if (idSesion != null && !idSesion.isEmpty()) {
            // Si hay una sesión, obtener el pago existente
            pagoVO = pagoGestionService.obtenerPagoPorIdSesion(idSesion);
            pagoDTO = PagoDTO.from(pagoVO);
        } else {
            // Si no hay sesión, crear uno nuevo
            pagoVO = pagoGestionService.toPagoVO(reserva);
            String nuevaSesion = "sess_" + java.util.UUID.randomUUID().toString().substring(0, 10);
            pagoDTO.setReservaId(reservaId);
            pagoDTO.setMonto(reserva.getPrecioTotal());
            pagoDTO.setIdSesion(nuevaSesion);
            pagoDTO.setMoneda("EUR");
        }
        
        model.addAttribute("reserva", reserva);
        model.addAttribute("pago", pagoDTO);
        
        return "pago/formulario-pago";
    }
    
    /**
     * Procesa el formulario de pago
     */
    @PostMapping("/procesar")
    public String procesarPago(@Valid @ModelAttribute("pago") PagoDTO pagoDTO,
                               Principal principal,
                               RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        
        try {
            // Obtener la reserva
            ReservaDTO reserva = reservaService.obtenerReservaPorId(pagoDTO.getReservaId());
            
            // Verificar que el usuario es el inquilino
            if (!reserva.getNombreUsuario().equals(principal.getName())) {
                ra.addFlashAttribute("error", "No tienes permiso para realizar este pago");
                return "redirect:/reservas/mis-reservas";
            }
            
            // Crear o actualizar el pago
            PagoVO pagoVO = pagoGestionService.crearPagoPendiente(
                    pagoDTO.getReservaId(), principal.getName(), pagoDTO.getIdSesion());
            
            // Simular procesamiento del pago (en producción, se usaría Stripe)
            pagoGestionService.onCheckoutSessionCompleted(pagoDTO.getIdSesion());
            
            // Actualizar el estado de la reserva
            reservaService.registrarPago(pagoDTO.getReservaId(), principal.getName());
            
            // Redireccionar a la página de éxito
            return "redirect:/pago/exito?idSesion=" + pagoDTO.getIdSesion();
            
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Error al procesar el pago: " + e.getMessage());
            return "redirect:/pago/fallido?idSesion=" + pagoDTO.getIdSesion() + "&error=" + e.getMessage();
        }
    }

    /**
     * Inicia el proceso de pago directo (sin formulario)
     */
    @GetMapping("/iniciar")
    public void iniciarPago(@RequestParam Long reservaId,
                            Principal principal,
                            HttpServletResponse response) throws Exception {
        if (principal == null) {
            response.sendRedirect("/login");
            return;
        }
        ReservaDTO reserva = reservaService.obtenerReservaPorId(reservaId);
        
        // Verificar que el usuario es el inquilino
        if (!reserva.getNombreUsuario().equals(principal.getName())) {
            response.sendRedirect("/error?mensaje=No tienes permiso para realizar este pago");
            return;
        }
        
        PagoVO pagoVO = pagoGestionService.toPagoVO(reserva);
        Session session = pagoService.crearSesionPago(pagoVO);
        pagoGestionService.crearPagoPendiente(reservaId,
                                              principal.getName(),
                                              session.getId());
        response.sendRedirect(session.getUrl());
    }

    @GetMapping("/simulador")
    public String simuladorPago(@RequestParam Long reservaId,
                                @RequestParam String idSesion,
                                Model model,
                                Principal principal) {
        if (principal == null) return "redirect:/login";
        ReservaDTO reserva = reservaService.obtenerReservaPorId(reservaId);
        if (!reserva.getNombreUsuario().equals(principal.getName())) {
            model.addAttribute("error", "No tienes permiso");
            return "error";
        }
        PagoVO pago = pagoGestionService.obtenerPagoPorIdSesion(idSesion);
        model.addAttribute("pago", pago);
        model.addAttribute("reserva", reserva);
        model.addAttribute("esSimulador", true);
        return "pago/simulador-pago";
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> procesarWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        try {
            Event event = Webhook.constructEvent(payload,
                                                 sigHeader,
                                                 stripeConfig.getWebhookSecret());
            if ("checkout.session.completed".equals(event.getType())) {
                String sessionId = ((Session) event.getDataObjectDeserializer()
                                        .getObject().orElseThrow()).getId();
                pagoGestionService.onCheckoutSessionCompleted(sessionId);
            }
            return ResponseEntity.ok("");
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Firma no válida");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        }
    }

    @GetMapping("/exito")
    public String pagoExitoso(@RequestParam("idSesion") String idSesion,
                              Model model,
                              Principal principal) {
        if (principal == null) return "redirect:/login";
        PagoVO pago = pagoGestionService.verificarYActualizarPago(idSesion);
        if (!pago.getUsuario().getUsername().equals(principal.getName())) {
            model.addAttribute("error", "No tienes permiso");
            return "error";
        }
        ReservaDTO reserva = reservaService.obtenerReservaPorId(pago.getReserva().getId());
        
        // Registrar el pago en la reserva (cambia el estado a PAGO_VERIFICADO)
        try {
            reservaService.registrarPago(reserva.getId(), principal.getName());
        } catch (Exception e) {
            // Si ya estaba registrado, ignoramos el error
        }
        
        model.addAttribute("pago", pago);
        model.addAttribute("reserva", reserva);
        return "pago/pago-exitoso";
    }

    @GetMapping("/fallido")
    public String pagoFallido(@RequestParam("idSesion") String idSesion,
                              @RequestParam(required = false) String error,
                              Model model,
                              Principal principal) {
        if (principal == null) return "redirect:/login";
        PagoVO pago = pagoGestionService.obtenerPagoPorIdSesion(idSesion);
        if (!pago.getUsuario().getUsername().equals(principal.getName())) {
            model.addAttribute("error", "No tienes permiso");
            return "error";
        }
        ReservaDTO reserva = reservaService.obtenerReservaPorId(pago.getReserva().getId());
        model.addAttribute("pago", pago);
        model.addAttribute("reserva", reserva);
        model.addAttribute("errorPago", error);
        return "pago/pago-fallido";
    }

    @GetMapping("/reserva/{reservaId}")
    public String listarPagos(@PathVariable Long reservaId,
                              Model model,
                              Principal principal) {
        if (principal == null) return "redirect:/login";
        ReservaDTO reserva = reservaService.obtenerReservaPorId(reservaId);
        if (!reserva.getNombreUsuario().equals(principal.getName())) {
            model.addAttribute("error", "No tienes permiso");
            return "error";
        }
        List<PagoVO> pagos = pagoGestionService.obtenerPagosPorReserva(reservaId);
        model.addAttribute("pagos", pagos);
        model.addAttribute("reserva", reserva);
        return "pago/lista-pagos";
    }

    @PostMapping("/cancelar/{pagoId}")
    public String cancelarPago(@PathVariable Long pagoId,
                               Principal principal,
                               RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        boolean ok = pagoGestionService.cancelarPago(pagoId, principal.getName());
        ra.addFlashAttribute(ok ? "mensajeExito" : "error",
                            ok ? "Pago cancelado correctamente" : "No se pudo cancelar");
        return "redirect:/reservas/mis-reservas";
    }

    @PostMapping("/reembolsar/{pagoId}")
    public String reembolsarPago(@PathVariable Long pagoId,
                                 @RequestParam String motivo,
                                 Principal principal,
                                 RedirectAttributes ra) {
        if (principal == null) return "redirect:/login";
        boolean ok = pagoGestionService.reembolsarPago(pagoId, motivo, principal.getName());
        ra.addFlashAttribute(ok ? "mensajeExito" : "error",
                            ok ? "Pago reembolsado correctamente" : "No se pudo reembolsar");
        return "redirect:/reservas/solicitudes";
    }
}