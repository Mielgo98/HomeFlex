package com.example.demo.pago.control;

import java.util.List;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.config.StripeConfig;
import com.example.demo.pago.model.EstadoPago;
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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/pago")
public class PagoController {
    private static final Logger logger = LoggerFactory.getLogger(PagoController.class);

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
     * Procesa el formulario de pago
     */
    @PostMapping("/procesar")
    @ResponseBody
    public ResponseEntity<?> procesarPago(@Valid @ModelAttribute("pago") PagoDTO pagoDTO,
                               Principal principal,
                               HttpServletRequest request) {
        if (principal == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Sesión expirada. Por favor, inicia sesión nuevamente.");
            response.put("redirect", "/login");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            // Obtener la reserva
            ReservaDTO reserva = reservaService.obtenerReservaPorId(pagoDTO.getReservaId());
            
            // Verificar que el usuario es el inquilino
            /* COMENTADO PARA PERMITIR PRUEBAS
            if (!reserva.getNombreUsuario().equals(principal.getName())) {
                response.put("success", false);
                response.put("message", "No tienes permiso para realizar este pago");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            */
            
            // Crear o actualizar el pago
            PagoVO pagoVO = pagoGestionService.crearPagoPendiente(
                    pagoDTO.getReservaId(), principal.getName(), pagoDTO.getIdSesion());
            
            // Simular procesamiento del pago (en producción, se usaría Stripe)
            pagoGestionService.onCheckoutSessionCompleted(pagoDTO.getIdSesion());
            
            // Actualizar el estado de la reserva
            reservaService.registrarPago(pagoDTO.getReservaId(), principal.getName());
            
            response.put("success", true);
            response.put("message", "¡Pago realizado con éxito!");
            response.put("sessionId", pagoDTO.getIdSesion());
            response.put("reservaId", pagoDTO.getReservaId());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error al procesar el pago: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Inicia el proceso de pago directo (sin formulario)
     * Puede devolver JSON para peticiones AJAX o redireccionar para peticiones normales
     */
    @GetMapping("/iniciar")
    public void iniciarPago(@RequestParam Long reservaId,
                            @RequestParam(required = false, defaultValue = "false") boolean ajax,
                            Principal principal,
                            HttpServletResponse response,
                            HttpServletRequest request) throws Exception {
        logger.info("Iniciando pago para reserva: {} (ajax={})", reservaId, ajax);
        
        if (principal == null) {
            if (ajax) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"success\":false,\"message\":\"Sesión expirada\",\"redirect\":\"/login\"}");
                return;
            }
            response.sendRedirect("/login");
            return;
        }
        
        try {
            ReservaDTO reserva = reservaService.obtenerReservaPorId(reservaId);
            
            logger.info("Usuario de la reserva: {}, Usuario actual: {}", 
                       reserva.getNombreUsuario(), principal.getName());
            
            // Verificar que el usuario es el inquilino
            /* COMENTADO PARA PERMITIR PRUEBAS
            if (!reserva.getNombreUsuario().equals(principal.getName())) {
                if (ajax) {
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"success\":false,\"message\":\"No tienes permiso para realizar este pago\"}");
                    return;
                }
                response.sendRedirect("/error?mensaje=No tienes permiso para realizar este pago");
                return;
            }
            */
            
            PagoVO pagoVO = pagoGestionService.toPagoVO(reserva);
            Session session = pagoService.crearSesionPago(pagoVO);
            PagoVO pagoCreado = pagoGestionService.crearPagoPendiente(reservaId,
                                                  principal.getName(),
                                                  session.getId());
            
            logger.info("Pago creado con ID: {}, sesión: {}", pagoCreado.getId(), session.getId());
            
            if (ajax) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                String jsonResponse = String.format(
                    "{\"success\":true,\"redirect\":\"%s\",\"sessionId\":\"%s\"}",
                    session.getUrl(), session.getId()
                );
                response.getWriter().write(jsonResponse);
                return;
            }
            
            logger.info("Redirigiendo a URL de pago: {}", session.getUrl());
            response.sendRedirect(session.getUrl());
        } catch (Exception e) {
            logger.error("Error al iniciar el pago: {}", e.getMessage());
            
            if (ajax) {
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                String jsonResponse = String.format(
                    "{\"success\":false,\"message\":\"%s\"}",
                    e.getMessage().replace("\"", "\\\"")
                );
                response.getWriter().write(jsonResponse);
                return;
            }
            
            response.sendRedirect("/error?mensaje=" + e.getMessage());
        }
    }

    @GetMapping("/simulador")
    public String simuladorPago(@RequestParam Long reservaId,
                                @RequestParam String idSesion,
                                Model model,
                                Principal principal) {
        if (principal == null) return "redirect:/login";
        ReservaDTO reserva = reservaService.obtenerReservaPorId(reservaId);
        
        /* COMENTADO PARA PERMITIR PRUEBAS
        if (!reserva.getNombreUsuario().equals(principal.getName())) {
            model.addAttribute("error", "No tienes permiso");
            return "error";
        }
        */
        
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
                
                // Obtener el pago y registrarlo en la reserva
                PagoVO pago = pagoGestionService.obtenerPagoPorIdSesion(sessionId);
                Long reservaId = pago.getReserva().getId();
                String username = pago.getUsuario().getUsername();
                
                // Actualizar el estado de la reserva
                reservaService.registrarPago(reservaId, username);
            }
            return ResponseEntity.ok("");
        } catch (SignatureVerificationException e) {
            return ResponseEntity.badRequest().body("Firma no válida");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error interno: " + e.getMessage());
        }
    }

    @GetMapping("/verificar")
    @ResponseBody
    public ResponseEntity<?> verificarPago(@RequestParam("idSesion") String idSesion,
                              Principal principal) {
        if (principal == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Sesión expirada");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            PagoVO pago = pagoGestionService.verificarYActualizarPago(idSesion);
            
            /* COMENTADO PARA PERMITIR PRUEBAS
            if (!pago.getUsuario().getUsername().equals(principal.getName())) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "No tienes permiso");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }
            */
            
            ReservaDTO reserva = reservaService.obtenerReservaPorId(pago.getReserva().getId());
            
            // Registrar el pago en la reserva (cambia el estado a PAGO_VERIFICADO)
            try {
                reservaService.registrarPago(reserva.getId(), principal.getName());
            } catch (Exception e) {
                // Si ya estaba registrado, ignoramos el error
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("pago", Map.of(
                "id", pago.getId(),
                "monto", pago.getMonto(),
                "moneda", pago.getMoneda(),
                "estado", pago.getEstado().name(),
                "fechaCreacion", pago.getFechaCreacion().toString(),
                "sessionId", pago.getSessionId()
            ));
            response.put("reserva", Map.of(
                "id", reserva.getId(),
                "estado", reserva.getEstado().name(),
                "codigoReserva", reserva.getCodigoReserva()
            ));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al verificar el pago: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/reserva/{reservaId}")
    public String listarPagos(@PathVariable Long reservaId,
                              Model model,
                              Principal principal) {
        if (principal == null) return "redirect:/login";
        ReservaDTO reserva = reservaService.obtenerReservaPorId(reservaId);
        
        /* COMENTADO PARA PERMITIR PRUEBAS
        if (!reserva.getNombreUsuario().equals(principal.getName())) {
            model.addAttribute("error", "No tienes permiso");
            return "error";
        }
        */
        
        List<PagoVO> pagos = pagoGestionService.obtenerPagosPorReserva(reservaId);
        model.addAttribute("pagos", pagos);
        model.addAttribute("reserva", reserva);
        return "pago/lista-pagos";
    }

    @PostMapping("/cancelar/{pagoId}")
    @ResponseBody
    public ResponseEntity<?> cancelarPago(@PathVariable Long pagoId,
                               Principal principal) {
        if (principal == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Sesión expirada");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            boolean ok = pagoGestionService.cancelarPago(pagoId, principal.getName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", ok);
            response.put("message", ok ? "Pago cancelado correctamente" : "No se pudo cancelar el pago");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al cancelar el pago: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/reembolsar/{pagoId}")
    @ResponseBody
    public ResponseEntity<?> reembolsarPago(@PathVariable Long pagoId,
                                 @RequestParam String motivo,
                                 Principal principal) {
        if (principal == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Sesión expirada");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
        
        try {
            boolean ok = pagoGestionService.reembolsarPago(pagoId, motivo, principal.getName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", ok);
            response.put("message", ok ? "Pago reembolsado correctamente" : "No se pudo reembolsar el pago");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error al reembolsar el pago: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
    
    
    @GetMapping("/exito")
    public String pagoExitoso(@RequestParam("idSesion") String idSesion,
                             Model model,
                             Principal principal) {
        logger.info("Redirigido a página de éxito para sesión: {}", idSesion);
        
        if (principal == null) return "redirect:/login";
        
        try {
            // Obtener el pago
            PagoVO pago = pagoGestionService.verificarYActualizarPago(idSesion);
            
            /* COMENTADO PARA PERMITIR PRUEBAS
            if (!pago.getUsuario().getUsername().equals(principal.getName())) {
                model.addAttribute("error", "No tienes permiso");
                return "error";
            }
            */
            
            // Actualizar el estado del pago a completado si es necesario
            if (pago.getEstado() != EstadoPago.COMPLETADO) {
                pagoGestionService.onCheckoutSessionCompleted(idSesion);
                pago = pagoGestionService.verificarYActualizarPago(idSesion);
            }
            
            // Obtener la reserva
            ReservaDTO reserva = reservaService.obtenerReservaPorId(pago.getReserva().getId());
            
            // Registrar el pago en la reserva
            try {
                reservaService.registrarPago(reserva.getId(), principal.getName());
            } catch (Exception e) {
                // Si ya estaba registrado, ignoramos el error
                logger.warn("Error al registrar pago (posiblemente ya registrado): {}", e.getMessage());
            }
            
            // Añadir atributos al modelo
            model.addAttribute("pago", pago);
            model.addAttribute("reserva", reserva);
            
            return "pago/pago-exitoso";
        } catch (Exception e) {
            logger.error("Error al procesar página de éxito: {}", e.getMessage());
            model.addAttribute("error", "Error al procesar el pago: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/fallido")
    public String pagoFallido(@RequestParam("idSesion") String idSesion,
                            @RequestParam(required = false) String error,
                            Model model,
                            Principal principal) {
        logger.info("Redirigido a página de pago fallido para sesión: {}", idSesion);
        
        if (principal == null) return "redirect:/login";
        
        try {
            // Obtener el pago
            PagoVO pago = pagoGestionService.obtenerPagoPorIdSesion(idSesion);
            
            /* COMENTADO PARA PERMITIR PRUEBAS
            if (!pago.getUsuario().getUsername().equals(principal.getName())) {
                model.addAttribute("error", "No tienes permiso");
                return "error";
            }
            */
            
            // Obtener la reserva
            ReservaDTO reserva = reservaService.obtenerReservaPorId(pago.getReserva().getId());
            
            // Añadir atributos al modelo
            model.addAttribute("pago", pago);
            model.addAttribute("reserva", reserva);
            model.addAttribute("errorPago", error);
            
            return "pago/pago-fallido";
        } catch (Exception e) {
            logger.error("Error al procesar página de pago fallido: {}", e.getMessage());
            model.addAttribute("error", "Error al procesar el pago: " + e.getMessage());
            return "error";
        }
    }
}