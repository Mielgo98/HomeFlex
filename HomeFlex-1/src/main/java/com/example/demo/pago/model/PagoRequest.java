package com.example.demo.pago.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Representa una solicitud de pago
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoRequest {
    private BigDecimal amount; //importe
    private String currency;
    private String paymentMethodId;
    private String description;
    private String customerEmail;
    private String returnUrl;
    private String cancelUrl;
}
