package com.example.demo.pago.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la información del pago en el formulario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoDTO {
    private Long reservaId;
    private String numeroTarjeta;
    private String cvv;
    private String fechaExpiracion;
    private String titular;
    private BigDecimal monto;
    private String idSesion;
    private String moneda = "EUR"; // Por defecto
}
