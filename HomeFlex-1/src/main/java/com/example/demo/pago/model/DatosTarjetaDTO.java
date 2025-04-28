package com.example.demo.pago.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Datos de tarjeta para el procesamiento de pago
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DatosTarjetaDTO {
    private String numeroTarjeta;
    private String cvv;
    private String mesExpiracion;
    private String anioExpiracion;
    private String titular;
}
