package com.example.demo.reserva.model;

/**
 * Representa los diferentes estados en los que puede encontrarse una reserva
 */
public enum EstadoReserva {
    SOLICITADA,      // Solicitud inicial enviada por el inquilino
    PENDIENTE_PAGO,  // Aprobada por el propietario, pendiente de pago
    PAGO_VERIFICADO, // Pago realizado y verificado
    CONFIRMADA,      // Reserva completamente confirmada
    CANCELADA        // Reserva cancelada (por cualquiera de las partes o por falta de pago)
}