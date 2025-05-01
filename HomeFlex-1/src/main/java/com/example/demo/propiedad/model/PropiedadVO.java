package com.example.demo.propiedad.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.foto.model.FotoVO;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "propiedad")
public class PropiedadVO {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude // Excluye la propiedad propietario de los métodos equals y hashCode
    private UsuarioVO propietario;
    
    @Column(length = 100, nullable = false)
    private String titulo;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String descripcion;
    
    @Column(name = "precio_dia", precision = 10, scale = 2, nullable = false)
    private BigDecimal precioDia;
    
    @Column(name = "precio_semana", precision = 10, scale = 2)
    private BigDecimal precioSemana;
    
    @Column(length = 255, nullable = false)
    private String direccion;
    
    @Column(length = 100, nullable = false)
    private String ciudad;
    
    @Column(length = 100, nullable = false)
    private String pais;
    
    @Column(precision = 10, scale = 8)
    private BigDecimal latitud;
    
    @Column(precision = 11, scale = 8)
    private BigDecimal longitud;
    
    @Column(nullable = false)
    private Integer capacidad;
    
    @Column(nullable = false)
    private Integer dormitorios;
    
    @Column(nullable = false)
    private Integer banos;
    
    @Column(nullable = false)
    private Boolean activo = true;
    
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
    
    // Relación con las fotos - se excluye para evitar recursión en hashCode y equals
    @OneToMany(mappedBy = "propiedad", fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<FotoVO> fotos = new HashSet<>();
    
    // Método para obtener la foto principal
    public String getFotoPrincipal() {
        return fotos.stream()
                .filter(FotoVO::isPrincipal)
                .findFirst()
                .map(FotoVO::getUrl)
                .orElse("/images/property-placeholder.jpg"); // Imagen por defecto
    }
    
    public PropiedadVO(PropiedadDTO dto) {
        this.id = dto.getId();
        this.titulo = dto.getTitulo();
        this.descripcion = dto.getDescripcion();
        this.precioDia = dto.getPrecioDia();
        this.ciudad = dto.getCiudad();
        this.pais = dto.getPais();
        this.capacidad = dto.getCapacidad();
        this.dormitorios = dto.getDormitorios();
        this.banos = dto.getBanos();
        // Quedamos que la foto principal se gestiona en la capa DTO/Controller,
        // así que aquí no la almacenamos; si necesitas guardarla, añade el campo correspondiente.
    }
}