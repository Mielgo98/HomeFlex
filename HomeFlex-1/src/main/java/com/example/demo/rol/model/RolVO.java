package com.example.demo.rol.model;

import java.util.HashSet;
import java.util.Set;

import com.example.demo.usuario.model.UsuarioVO;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "rol")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RolVO {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String nombre;
    
    // Relación inversa para ManyToMany con Usuario
    // Evitamos ciclos de serialización
    @ManyToMany(mappedBy = "roles")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<UsuarioVO> usuarios = new HashSet<>();
    
    /**
     * Override del método equals para usar solo el ID de la entidad
     * Esto evita problemas con relaciones bidireccionales
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RolVO)) return false;
        RolVO that = (RolVO) o;
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