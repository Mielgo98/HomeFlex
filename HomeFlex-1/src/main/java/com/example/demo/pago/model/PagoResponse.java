package com.example.demo.pago.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa la respuesta a una solicitud de pago
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoResponse {
    private String idSesion;
    private String idPago;
    private BigDecimal monto;
    private String resultado;     // success, failed, pending
    private String mensaje;
    private String codigoError;
    private String urlRedireccion; 
}