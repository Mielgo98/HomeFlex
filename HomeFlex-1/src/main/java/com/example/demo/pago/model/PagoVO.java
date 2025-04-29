package com.example.demo.pago.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.example.demo.reserva.model.ReservaVO;
import com.example.demo.usuario.model.UsuarioVO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

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
    
    @ManyToOne
    @JoinColumn(name = "reserva_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private ReservaVO reserva;
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UsuarioVO usuario;
    
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal monto;
    
    @Column(nullable = false)
    private String moneda;
    
    @Column(name = "id_sesion", length = 100, unique = true)
    private String idSesion;
    
    @Column(name = "id_pago", length = 100)
    private String idPago;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPago estado;
    
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
    
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
    
    @Column(name = "metodo_pago", nullable = false)
    private String metodoPago;
    
    @Column(name = "ultimos_digitos", length = 4)
    private String ultimosDigitos;
    
    @Column(length = 500)
    private String detalles;
    
    /**
     * Override del método equals para usar solo el ID de la entidad
     * Esto evita problemas con relaciones bidireccionales
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PagoVO)) return false;
        PagoVO that = (PagoVO) o;
        return id != null && id.equals(that.getId());
    }

    /**
     * Override del método hashCode para usar solo el ID de la entidad
     * Esto evita problemas con relaciones bidireccionales
     */
    @Override
    public int hashCode() {
        // Se usa un valor constante si el ID es nulo
        return id != null ? id.hashCode() : 31;
    }
}