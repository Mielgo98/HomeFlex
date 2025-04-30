package com.example.demo.pago.model;

/**
 * Enumera los posibles estados de un pago
 */
public enum EstadoPago {
    PENDIENTE,
    PROCESANDO,
    COMPLETADO,
    FALLIDO,
    REEMBOLSADO,
    CANCELADO
}
