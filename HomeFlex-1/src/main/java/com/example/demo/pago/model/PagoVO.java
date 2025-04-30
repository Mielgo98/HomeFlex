package com.example.demo.pago.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.reserva.model.ReservaVO;
import com.example.demo.usuario.model.UsuarioVO;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entidad para mantener un registro de pagos en la base de datos
 */
@Entity
@Table(name = "pago")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoVO {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne @JoinColumn(name = "reserva_id", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private ReservaVO reserva;

    @ManyToOne @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private UsuarioVO usuario;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(name = "session_id", length = 100, unique = true)
    private String sessionId;

    @Column(name = "payment_intent_id", length = 100)
    private String paymentIntentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPago estado;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @Column(length = 500)
    private String motivoReembolso;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PagoVO)) return false;
        PagoVO that = (PagoVO) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 31;
    }
}
