// PropiedadDTO.java
package com.example.demo.propiedad.model;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropiedadDTO {
    
    private Long id;
    private String titulo;
    private String descripcion;
    private BigDecimal precioDia;
    private String ciudad;
    private String pais;
    private Integer capacidad;
    private Integer dormitorios;
    private Integer banos;
    private String fotoPrincipal;
    
    // Constructor para convertir desde el VO
    public PropiedadDTO(PropiedadVO propiedad) {
        this.id = propiedad.getId();
        this.titulo = propiedad.getTitulo();
        this.descripcion = propiedad.getDescripcion();
        this.precioDia = propiedad.getPrecioDia();
        this.ciudad = propiedad.getCiudad();
        this.pais = propiedad.getPais();
        this.capacidad = propiedad.getCapacidad();
        this.dormitorios = propiedad.getDormitorios();
        this.banos = propiedad.getBanos();
        this.fotoPrincipal = propiedad.getFotoPrincipal();
    }
}