package com.example.demo.pago.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.pago.model.EstadoPago;
import com.example.demo.pago.model.PagoVO;
import com.example.demo.pago.repository.PagoRepository;
import com.example.demo.reserva.model.ReservaDTO;
import com.example.demo.reserva.model.ReservaVO;
import com.example.demo.reserva.service.ReservaService;
import com.example.demo.usuario.model.UsuarioVO;

@Service
public class PagoGestionService {

    private final PagoRepository pagoRepository;
    private final ReservaService reservaService;

    @Value("${pago.moneda.defecto}")
    private String defaultCurrency;

    public PagoGestionService(PagoRepository pagoRepository,
                              ReservaService reservaService) {
        this.pagoRepository = pagoRepository;
        this.reservaService = reservaService;
    }

    /**
     * Crea un PagoVO provisional a partir de ReservaDTO
     */
    public PagoVO toPagoVO(ReservaDTO dto) {
        PagoVO pago = new PagoVO();
        // Referencia mínima a Reserva
        ReservaVO reservaVo = new ReservaVO();
        reservaVo.setId(dto.getId());
        pago.setReserva(reservaVo);
        // Referencia mínima a Usuario
        UsuarioVO userVo = new UsuarioVO();
        userVo.setId(dto.getUsuarioId());
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
        ReservaDTO dto = reservaService.obtenerReservaPorId(reservaId);

        PagoVO pago = new PagoVO();
        ReservaVO reservaVo = new ReservaVO();
        reservaVo.setId(dto.getId());
        pago.setReserva(reservaVo);

        UsuarioVO userVo = new UsuarioVO();
        userVo.setId(dto.getUsuarioId());
        pago.setUsuario(userVo);

        pago.setMonto(dto.getPrecioTotal());
        pago.setMoneda(defaultCurrency);
        pago.setSessionId(sessionId);
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setFechaCreacion(LocalDateTime.now());

        return pagoRepository.save(pago);
    }

    @Transactional
    public void onCheckoutSessionCompleted(String sessionId) {
        PagoVO pago = pagoRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado: " + sessionId));
        pago.setEstado(EstadoPago.COMPLETADO);
        pago.setFechaActualizacion(LocalDateTime.now());
        pagoRepository.save(pago);
    }

    @Transactional
    public PagoVO verificarYActualizarPago(String sessionId) {
        return pagoRepository.findBySessionId(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado: " + sessionId));
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
        PagoVO pago = pagoRepository.findById(pagoId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado: " + pagoId));
        if (!pago.getUsuario().getUsername().equals(username) ||
            pago.getEstado() != EstadoPago.PENDIENTE) {
            return false;
        }
        pago.setEstado(EstadoPago.CANCELADO);
        pago.setFechaActualizacion(LocalDateTime.now());
        pagoRepository.save(pago);
        return true;
    }

    @Transactional
    public boolean reembolsarPago(Long pagoId, String motivo, String username) {
        PagoVO pago = pagoRepository.findById(pagoId)
            .orElseThrow(() -> new IllegalArgumentException("Pago no encontrado: " + pagoId));
        if (pago.getEstado() != EstadoPago.COMPLETADO) {
            return false;
        }
        pago.setEstado(EstadoPago.REEMBOLSADO);
        pago.setFechaActualizacion(LocalDateTime.now());
        pago.setMotivoReembolso(motivo);
        pagoRepository.save(pago);
        return true;
    }
}
