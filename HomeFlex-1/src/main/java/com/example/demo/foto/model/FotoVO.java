package com.example.demo.foto.model;

import com.example.demo.propiedad.model.PropiedadVO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(name = "foto")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class FotoVO {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Relacion con la propiedad - excluida del hashCode y equals para evitar recursión
    @ManyToOne
    @JoinColumn(name = "propiedad_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private PropiedadVO propiedad;
    
    @Column(nullable = false, length = 255)
    private String url;
    
    @Column(length = 255)
    private String descripcion;
    
    @Column(nullable = false)
    private boolean principal;
}