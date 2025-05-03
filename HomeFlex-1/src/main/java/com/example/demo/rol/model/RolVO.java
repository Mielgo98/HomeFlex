package com.example.demo.rol.model;

import java.util.HashSet;
import java.util.Set;
import com.example.demo.menu.model.MenuItemVO;
import com.example.demo.usuario.model.UsuarioVO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

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
    
    @ManyToMany(mappedBy = "roles")
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<UsuarioVO> usuarios = new HashSet<>();
    
    @ManyToMany(mappedBy = "roles")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<MenuItemVO> menuItems = new HashSet<>();
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RolVO)) return false;
        RolVO that = (RolVO) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 31;
    }
}
