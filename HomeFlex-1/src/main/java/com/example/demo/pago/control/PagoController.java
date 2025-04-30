package com.example.demo.pago.control;

import java.util.List;
import java.security.Principal;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.config.StripeConfig;
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

    @GetMapping("/iniciar")
    public void iniciarPago(@RequestParam Long reservaId,
                            Principal principal,
                            HttpServletResponse response) throws Exception {
        if (principal == null) {
            response.sendRedirect("/login");
            return;
        }
        ReservaDTO reserva = reservaService.obtenerReservaPorId(reservaId);
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
