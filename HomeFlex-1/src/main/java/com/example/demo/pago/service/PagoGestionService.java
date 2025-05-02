package com.example.demo.pago.service;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.config.StripeConfig;
import com.example.demo.pago.model.EstadoPago;
import com.example.demo.pago.model.PagoVO;
import com.example.demo.pago.repository.PagoRepository;
import com.example.demo.reserva.model.ReservaDTO;
import com.example.demo.reserva.model.ReservaVO;
import com.example.demo.reserva.service.ReservaService;
import com.example.demo.usuario.model.UsuarioVO;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;

@Service
public class PagoGestionService {
    private static final Logger logger = LoggerFactory.getLogger(PagoGestionService.class);

    @Autowired
    private PagoRepository pagoRepository;
    
    @Autowired
    private ReservaService reservaService;
    
    @Autowired
    private StripeConfig stripeConfig;
    
    @Autowired
    private PagoService pagoService;

    @Value("${pago.moneda.defecto}")
    private String defaultCurrency;

    /**
     * Crea un PagoVO provisional a partir de ReservaDTO
     */
    public PagoVO toPagoVO(ReservaDTO dto) {
        logger.debug("Creando PagoVO para reserva ID: {}", dto.getId());
        
        PagoVO pago = new PagoVO();
        // Referencia mínima a Reserva
        ReservaVO reservaVo = new ReservaVO();
        reservaVo.setId(dto.getId());
        pago.setReserva(reservaVo);
        // Referencia mínima a Usuario
        UsuarioVO userVo = new UsuarioVO();
        userVo.setId(dto.getUsuarioId());
        userVo.setUsername(dto.getNombreUsuario());
        // No incluimos email ya que no está disponible en el DTO
        pago.setUsuario(userVo);
        pago.setMonto(dto.getPrecioTotal());
        pago.setMoneda(defaultCurrency);
        return pago;
    }
    /**
     * Crea y persiste un pago en estado PENDIENTE antes de redirigir al checkout.
     */
    @Transactional
    public PagoVO crearPagoPendiente(Long reservaId, String username, String sessionId) {
        logger.info("Creando pago pendiente para reserva: {}, usuario: {}, sesión: {}", 
                   reservaId, username, sessionId);
                   
        ReservaDTO dto = reservaService.obtenerReservaPorId(reservaId);

        PagoVO pago = new PagoVO();
        ReservaVO reservaVo = new ReservaVO();
        reservaVo.setId(dto.getId());
        pago.setReserva(reservaVo);

        UsuarioVO userVo = new UsuarioVO();
        userVo.setId(dto.getUsuarioId());
        userVo.setUsername(username);
        pago.setUsuario(userVo);

        pago.setMonto(dto.getPrecioTotal());
        pago.setMoneda(defaultCurrency);
        pago.setSessionId(sessionId);
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setFechaCreacion(LocalDateTime.now());

        PagoVO saved = pagoRepository.save(pago);
        logger.info("Pago pendiente creado con ID: {}", saved.getId());
        return saved;
    }

    /**
     * Procesa una sesión completada de Stripe
     */
    @Transactional
    public void onCheckoutSessionCompleted(String sessionId) {
        logger.info("Procesando sesión completada: {}", sessionId);
        
        PagoVO pago = pagoRepository.findBySessionId(sessionId)
            .orElseThrow(() -> {
                logger.error("Pago no encontrado para sessionId: {}", sessionId);
                return new IllegalArgumentException("Pago no encontrado: " + sessionId);
            });
            
        // Si no estamos en sandbox, actualizamos el paymentIntentId desde Stripe
        if (!stripeConfig.isUseSandbox()) {
            try {
                Session session = pagoService.obtenerSesion(sessionId);
                String paymentIntentId = session.getPaymentIntent();
                pago.setPaymentIntentId(paymentIntentId);
                logger.info("PaymentIntent ID actualizado: {}", paymentIntentId);
            } catch (StripeException e) {
                logger.error("Error al obtener detalles de la sesión: {}", e.getMessage());
            }
        }
        
        pago.setEstado(EstadoPago.COMPLETADO);
        pago.setFechaActualizacion(LocalDateTime.now());
        pagoRepository.save(pago);
        logger.info("Pago actualizado a estado COMPLETADO");
    }

    @Transactional
    public PagoVO verificarYActualizarPago(String sessionId) {
        logger.info("Verificando pago para sesión: {}", sessionId);
        return pagoRepository.findBySessionId(sessionId)
            .orElseThrow(() -> {
                logger.error("Pago no encontrado para sessionId: {}", sessionId);
                return new IllegalArgumentException("Pago no encontrado: " + sessionId);
            });
    }

    public List<PagoVO> obtenerPagosPorReserva(Long reservaId) {
        return pagoRepository.findAllByReservaId(reservaId);
    }

    public PagoVO obtenerPagoPorIdSesion(String sessionId) {
        return pagoRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado: " + sessionId));
    }

    @Transactional
    public boolean cancelarPago(Long pagoId, String username) {
        logger.info("Intentando cancelar pago ID: {} por usuario: {}", pagoId, username);
        
        PagoVO pago = pagoRepository.findById(pagoId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado: " + pagoId));
            
        if (!pago.getUsuario().getUsername().equals(username) ||
            pago.getEstado() != EstadoPago.PENDIENTE) {
            logger.warn("No se puede cancelar: usuario incorrecto o estado no es PENDIENTE");
            return false;
        }
        
        pago.setEstado(EstadoPago.CANCELADO);
        pago.setFechaActualizacion(LocalDateTime.now());
        pagoRepository.save(pago);
        logger.info("Pago cancelado correctamente");
        return true;
    }

    @Transactional
    public boolean reembolsarPago(Long pagoId, String motivo, String username) {
        logger.info("Intentando reembolsar pago ID: {} por usuario: {}", pagoId, username);
        
        PagoVO pago = pagoRepository.findById(pagoId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado: " + pagoId));
            
        if (pago.getEstado() != EstadoPago.COMPLETADO) {
            logger.warn("No se puede reembolsar: el estado no es COMPLETADO");
            return false;
        }
        
        // Procesar el reembolso con Stripe (en producción)
        if (!stripeConfig.isUseSandbox() && pago.getPaymentIntentId() != null) {
            boolean reembolsoExitoso = pagoService.procesarReembolso(pago, motivo);
            if (!reembolsoExitoso) {
                logger.error("Error al procesar el reembolso con Stripe");
                return false;
            }
        } else {
            // En sandbox solo actualizamos el estado
            pago.setEstado(EstadoPago.REEMBOLSADO);
            pago.setFechaActualizacion(LocalDateTime.now());
            pago.setMotivoReembolso(motivo);
            pagoRepository.save(pago);
        }
        
        logger.info("Pago reembolsado correctamente");
        return true;
    }
}