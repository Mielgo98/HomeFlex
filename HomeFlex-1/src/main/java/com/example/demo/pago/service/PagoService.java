package com.example.demo.pago.service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.pago.model.PagoVO;
import com.example.demo.pago.repository.PagoRepository;
import com.example.demo.pago.model.PagoDTO;
import com.example.demo.reserva.model.ReservaDTO;
import com.example.demo.reserva.service.ReservaService;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

@Service
public class PagoService {
    private static final Logger logger = LoggerFactory.getLogger(PagoService.class);

    @Value("${pago.api.key}")
    private String apiKey;

    @Value("${pago.use.sandbox}")
    private boolean useSandbox;

    @Value("${pago.sandbox.url}")
    private String sandboxUrl;

    @Value("${app.url}")
    private String appUrl;

    @Value("${pago.moneda.defecto:EUR}")
    private String defaultCurrency;

    @Autowired
    private PagoRepository pagoRepository;
    
    @Autowired
    private ReservaService reservaService;
    
    public Session crearSesionPago(PagoVO pago) throws StripeException {
        // Asegurarse de que la clave API está configurada
        Stripe.apiKey = apiKey;
        
        logger.info("Creando sesión de pago para reserva ID: {}", pago.getReserva().getId());
        
        if (useSandbox) {
            logger.info("Usando SIMULADOR de pago en modo sandbox");
            String sessionId = UUID.randomUUID().toString();
            Session fake = new Session();
            fake.setId(sessionId);
            fake.setUrl(appUrl + "/pago/simulador?reservaId="
                        + pago.getReserva().getId()
                        + "&idSesion=" + sessionId);
            return fake;
        }

        try {
            logger.info("Usando Stripe en modo real");
            BigDecimal amount = pago.getMonto().multiply(new BigDecimal(100));
            
            // Obtener más información sobre la reserva si está disponible
            ReservaDTO reserva = null;
            String descripcionReserva = "Reserva #" + pago.getReserva().getId();
            try {
                reserva = reservaService.obtenerReservaPorId(pago.getReserva().getId());
                if (reserva != null) {
                    descripcionReserva = String.format(
                        "Reserva de alojamiento en %s del %s al %s", 
                        reserva.getCiudadPropiedad(),
                        reserva.getFechaInicio(),
                        reserva.getFechaFin()
                    );
                }
            } catch (Exception e) {
                logger.warn("No se pudo obtener información detallada de la reserva: {}", e.getMessage());
            }

            // Crear metadatos básicos
            Map<String, String> metadata = new HashMap<>();
            metadata.put("reservaId", pago.getReserva().getId().toString());
            metadata.put("usuarioId", pago.getUsuario().getId().toString());
            
            // Crear la sesión de Stripe
            SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(appUrl + "/pago/exito?idSesion={CHECKOUT_SESSION_ID}")
                .setCancelUrl(appUrl + "/pago/fallido?idSesion={CHECKOUT_SESSION_ID}")
                .putAllMetadata(metadata)
                .addLineItem(
                    SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                            SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(defaultCurrency.toLowerCase())
                                .setUnitAmount(amount.longValue())
                                .setProductData(
                                    SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("Reserva #" + pago.getReserva().getId())
                                        .setDescription(descripcionReserva)
                                        .build())
                                .build())
                        .build())
                .build();

            Session session = Session.create(params);
            logger.info("Sesión de pago creada con ID: {}", session.getId());
            return session;
            
        } catch (StripeException e) {
            logger.error("Error creando sesión de pago con Stripe: {}", e.getMessage());
            throw e;
        }
    }
    
    public List<PagoDTO> obtenerPorReserva(Long reservaId) {
        return pagoRepository.findAllByReservaId(reservaId).stream()
                             .map(PagoDTO::from)   
                             .collect(Collectors.toList());
    }
    
    /**
     * Procesa un reembolso a través de Stripe
     */
    public boolean procesarReembolso(PagoVO pago, String motivo) {
        logger.info("Procesando reembolso para pago ID: {}", pago.getId());
        
        // En modo sandbox, simular el reembolso
        if (useSandbox) {
            logger.info("Simulando reembolso en modo sandbox para pago: {}", pago.getId());
            return true;
        }
        
        // En modo real, procesar con Stripe
        try {
            // Verificar que tengamos el paymentIntentId
            if (pago.getPaymentIntentId() == null) {
                logger.error("No se puede reembolsar un pago sin PaymentIntentId");
                return false;
            }
            
            // Asegurar que la API key está configurada
            Stripe.apiKey = apiKey;
            
            // Crear los parámetros del reembolso
            Map<String, Object> params = new HashMap<>();
            params.put("payment_intent", pago.getPaymentIntentId());
            // Añadir metadatos para seguimiento
            Map<String, String> metadata = new HashMap<>();
            metadata.put("motivo", motivo);
            metadata.put("reservaId", pago.getReserva().getId().toString());
            params.put("metadata", metadata);
            
            // Crear el reembolso
            com.stripe.model.Refund refund = com.stripe.model.Refund.create(params);
            logger.info("Reembolso procesado con ID: {}", refund.getId());
            
            return true;
        } catch (StripeException e) {
            logger.error("Error al procesar reembolso: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene detalles de una sesión de Stripe
     */
    public Session obtenerSesion(String sessionId) throws StripeException {
        logger.info("Obteniendo detalles de sesión: {}", sessionId);
        
        // En modo sandbox, simular la sesión
        if (useSandbox) {
            logger.info("Devolviendo sesión simulada para el ID: {}", sessionId);
            Session fakeSession = new Session();
            fakeSession.setId(sessionId);
            // Configurar datos simulados básicos
            return fakeSession;
        }
        
        // En modo real, consultar a Stripe
        try {
            // Asegurar que la API key está configurada
            Stripe.apiKey = apiKey;
            
            // Recuperar la sesión
            Session session = Session.retrieve(sessionId);
            logger.info("Sesión recuperada con ID: {}", session.getId());
            return session;
        } catch (StripeException e) {
            logger.error("Error al obtener detalles de la sesión: {}", e.getMessage());
            throw e;
        }
    }
}