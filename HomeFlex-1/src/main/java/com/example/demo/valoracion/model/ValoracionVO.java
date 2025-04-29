package com.example.demo.valoracion.model;

import java.time.LocalDateTime;

import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.usuario.model.UsuarioVO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Entidad que representa una valoración de un usuario sobre una propiedad
 */
@Entity
@Table(name = "valoracion")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValoracionVO {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "propiedad_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PropiedadVO propiedad;
    
    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private UsuarioVO usuario;
    
    @NotNull
    @Min(1)
    @Max(5)
    @Column(nullable = false)
    private Integer puntuacion; // Valoración de 1 a 5 estrellas
    
    @Column(columnDefinition = "TEXT")
    private String comentario;
    
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
    
    @Column(nullable = false)
    private Boolean aprobada;
    
    // Campos adicionales para evaluar aspectos específicos
    @Min(1)
    @Max(5)
    private Integer limpieza;
    
    @Min(1)
    @Max(5)
    private Integer ubicacion;
    
    @Min(1)
    @Max(5)
    private Integer comunicacion;
    
    @Min(1)
    @Max(5)
    private Integer calidad;
    
    // Campo para la respuesta del propietario
    @Column(name = "respuesta_propietario", columnDefinition = "TEXT")
    private String respuestaPropietario;
    
    @Column(name = "fecha_respuesta")
    private LocalDateTime fechaRespuesta;
    
    /**
     * Override del método equals para usar solo el ID de la entidad
     * Esto evita problemas con relaciones bidireccionales
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ValoracionVO)) return false;
        ValoracionVO that = (ValoracionVO) o;
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