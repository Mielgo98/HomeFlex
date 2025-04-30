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

    /**
     * Construye un PagoDTO a partir de un PagoVO
     */
    public static PagoDTO from(PagoVO vo) {
        PagoDTO dto = new PagoDTO();
        dto.setReservaId(vo.getReserva().getId());
        dto.setMonto(vo.getMonto());
        dto.setIdSesion(vo.getSessionId());
        dto.setMoneda(vo.getMoneda());
        return dto;
    }
}
