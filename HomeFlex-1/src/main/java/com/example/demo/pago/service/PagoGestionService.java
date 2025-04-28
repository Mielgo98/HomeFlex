package com.example.demo.pago.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.pago.model.EstadoPago;
import com.example.demo.pago.model.PagoDTO;
import com.example.demo.pago.model.PagoResponse;
import com.example.demo.pago.model.PagoVO;
import com.example.demo.pago.repository.PagoRepository;
import com.example.demo.reserva.model.EstadoReserva;
import com.example.demo.reserva.model.ReservaVO;
import com.example.demo.reserva.repository.ReservaRepository;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.repository.UsuarioRepository;

/**
 * Servicio para gestionar la lógica de negocio relacionada con pagos
 */
@Service
public class PagoGestionService {

    @Autowired
    private PagoRepository pagoRepository;
    
    @Autowired
    private ReservaRepository reservaRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PagoService pagoService;
    
    /**
     * Inicia un proceso de pago para una reserva
     */
    @Transactional
    public String iniciarProcesoDeCompra(Long reservaId, String username) {
        // Verificar que la reserva existe
        ReservaVO reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        
        // Verificar que el usuario es el inquilino de la reserva
        UsuarioVO usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (!reserva.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("No tienes permiso para realizar este pago");
        }
        
        // Verificar que la reserva está en un estado que permite el pago
        if (reserva.getEstado() != EstadoReserva.PENDIENTE_PAGO) {
            throw new RuntimeException("La reserva no está en estado de pago pendiente");
        }
        
        // Verificar que no haya un pago en proceso o completado
        boolean existePagoCompletado = pagoRepository.existsByReservaAndEstado(reserva, EstadoPago.COMPLETADO);
        if (existePagoCompletado) {
            throw new RuntimeException("Ya existe un pago completado para esta reserva");
        }
        
        // Crear sesión de pago
        String urlPago = pagoService.crearSesionPago(reserva);
        
        // Registrar el intento de pago en la base de datos
        PagoVO pago = new PagoVO();
        pago.setReserva(reserva);
        pago.setUsuario(usuario);
        pago.setMonto(reserva.getPrecioTotal());
        pago.setMoneda("EUR"); // Por defecto usamos euros
        pago.setIdSesion(urlPago.substring(urlPago.indexOf("idSesion=") + 9));
        pago.setEstado(EstadoPago.PENDIENTE);
        pago.setFechaCreacion(LocalDateTime.now());
        pago.setMetodoPago("TARJETA"); // Por defecto
        
        pagoRepository.save(pago);
        
        return urlPago;
    }
    
    /**
     * Procesa un pago con los datos de la tarjeta
     */
    @Transactional
    public PagoResponse procesarPago(PagoDTO pagoDTO, String username) {
        // Verificar que la reserva existe
        ReservaVO reserva = reservaRepository.findById(pagoDTO.getReservaId())
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        
        // Verificar que el usuario es el inquilino de la reserva
        UsuarioVO usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (!reserva.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("No tienes permiso para realizar este pago");
        }
        
        // Buscar si ya existe un pago pendiente para esta reserva
        Optional<PagoVO> pagoPendienteOpt = pagoRepository.findByReservaAndEstado(reserva, EstadoPago.PENDIENTE)
                .stream().findFirst();
        
        // Si no hay pago pendiente, creamos uno nuevo
        PagoVO pago;
        if (pagoPendienteOpt.isEmpty()) {
            pago = new PagoVO();
            pago.setReserva(reserva);
            pago.setUsuario(usuario);
            pago.setMonto(pagoDTO.getMonto());
            pago.setMoneda(pagoDTO.getMoneda());
            pago.setIdSesion(pagoDTO.getIdSesion());
            pago.setEstado(EstadoPago.PROCESANDO);
            pago.setFechaCreacion(LocalDateTime.now());
            pago.setMetodoPago("TARJETA");
            // Guardar los últimos 4 dígitos de la tarjeta por seguridad
            if (pagoDTO.getNumeroTarjeta() != null && pagoDTO.getNumeroTarjeta().length() >= 4) {
                pago.setUltimosDigitos(pagoDTO.getNumeroTarjeta().substring(pagoDTO.getNumeroTarjeta().length() - 4));
            }
        } else {
            pago = pagoPendienteOpt.get();
            pago.setEstado(EstadoPago.PROCESANDO);
            pago.setFechaActualizacion(LocalDateTime.now());
        }
        
        pagoRepository.save(pago);
        
        // Procesar el pago con el servicio de pagos
        PagoResponse respuesta = pagoService.procesarPago(
                pagoDTO.getNumeroTarjeta(),
                pagoDTO.getCvv(),
                pagoDTO.getFechaExpiracion(),
                pagoDTO.getMonto(),
                pagoDTO.getIdSesion());
        
        // Actualizar el pago con la respuesta
        pago.setIdPago(respuesta.getIdPago());
        
        if ("success".equals(respuesta.getResultado())) {
            pago.setEstado(EstadoPago.COMPLETADO);
            
            // Actualizar el estado de la reserva
            reserva.setEstado(EstadoReserva.PAGO_VERIFICADO);
            reservaRepository.save(reserva);
        } else {
            pago.setEstado(EstadoPago.FALLIDO);
            pago.setDetalles(respuesta.getCodigoError() + ": " + respuesta.getMensaje());
        }
        
        pago.setFechaActualizacion(LocalDateTime.now());
        pagoRepository.save(pago);
        
        return respuesta;
    }
    
    /**
     * Obtiene los detalles de un pago por su ID de sesión
     */
    public PagoVO obtenerPagoPorIdSesion(String idSesion) {
        return pagoRepository.findByIdSesion(idSesion)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
    }
    
    /**
     * Obtiene los detalles de un pago por su ID
     */
    public PagoVO obtenerPagoPorId(Long id) {
        return pagoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
    }
    
    /**
     * Obtiene todos los pagos de una reserva
     */
    public List<PagoVO> obtenerPagosPorReserva(Long reservaId) {
        ReservaVO reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        
        return pagoRepository.findByReserva(reserva);
    }
    
    /**
     * Verifica y actualiza el estado de un pago
     */
    @Transactional
    public PagoVO verificarYActualizarPago(String idSesion) {
        PagoVO pago = pagoRepository.findByIdSesion(idSesion)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
        
        // Si el pago ya está completado o fallido, no hacemos nada
        if (pago.getEstado() == EstadoPago.COMPLETADO || pago.getEstado() == EstadoPago.FALLIDO) {
            return pago;
        }
        
        // Verificar el estado del pago con el servicio de pagos
        PagoResponse respuesta = pagoService.verificarPago(idSesion);
        
        // Actualizar el estado del pago según la respuesta
        if ("success".equals(respuesta.getResultado())) {
            pago.setEstado(EstadoPago.COMPLETADO);
            pago.setIdPago(respuesta.getIdPago());
            
            // Actualizar el estado de la reserva
            ReservaVO reserva = pago.getReserva();
            reserva.setEstado(EstadoReserva.PAGO_VERIFICADO);
            reservaRepository.save(reserva);
        } else if ("failed".equals(respuesta.getResultado())) {
            pago.setEstado(EstadoPago.FALLIDO);
            pago.setDetalles(respuesta.getCodigoError() + ": " + respuesta.getMensaje());
        }
        
        pago.setFechaActualizacion(LocalDateTime.now());
        return pagoRepository.save(pago);
    }
    
    /**
     * Reembolsa un pago
     */
    @Transactional
    public boolean reembolsarPago(Long pagoId, String motivo, String username) {
        PagoVO pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
        
        // Verificar que el usuario es el propietario de la propiedad
        UsuarioVO usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        ReservaVO reserva = pago.getReserva();
        
        if (!reserva.getPropiedad().getPropietario().getId().equals(usuario.getId())) {
            throw new RuntimeException("No tienes permiso para reembolsar este pago");
        }
        
        // Verificar que el pago está completado
        if (pago.getEstado() != EstadoPago.COMPLETADO) {
            throw new RuntimeException("Solo se pueden reembolsar pagos completados");
        }
        
        // En un entorno real, aquí se haría la llamada al servicio de pagos para realizar el reembolso
        // Para esta simulación, simplemente actualizamos el estado
        
        pago.setEstado(EstadoPago.REEMBOLSADO);
        pago.setDetalles("Reembolsado: " + motivo);
        pago.setFechaActualizacion(LocalDateTime.now());
        pagoRepository.save(pago);
        
        // Actualizar el estado de la reserva
        reserva.setEstado(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);
        
        return true;
    }
    
    /**
     * Cancela un pago pendiente
     */
    @Transactional
    public boolean cancelarPago(Long pagoId, String username) {
        PagoVO pago = pagoRepository.findById(pagoId)
                .orElseThrow(() -> new RuntimeException("Pago no encontrado"));
        
        // Verificar que el usuario es el inquilino
        UsuarioVO usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        
        if (!pago.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("No tienes permiso para cancelar este pago");
        }
        
        // Verificar que el pago está pendiente o procesando
        if (pago.getEstado() != EstadoPago.PENDIENTE && pago.getEstado() != EstadoPago.PROCESANDO) {
            throw new RuntimeException("Solo se pueden cancelar pagos pendientes o en proceso");
        }
        
        pago.setEstado(EstadoPago.CANCELADO);
        pago.setFechaActualizacion(LocalDateTime.now());
        pagoRepository.save(pago);
        
        return true;
    }
    
    /**
     * Comprueba si una reserva tiene un pago completado
     */
    public boolean tienePagoCompletado(Long reservaId) {
        ReservaVO reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));
        
        return pagoRepository.existsByReservaAndEstado(reserva, EstadoPago.COMPLETADO);
    }
}