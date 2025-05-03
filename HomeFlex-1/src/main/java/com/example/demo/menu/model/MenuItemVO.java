package com.example.demo.menu.model;

import java.util.HashSet;
import java.util.Set;
import com.example.demo.rol.model.RolVO;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "menu_item")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemVO {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String nombre;
    
    @Column(length = 255)
    private String descripcion;
    
    @Column(nullable = false)
    private String url;
    
    @Column(length = 50)
    private String icono;
    
    @ManyToOne
    @JoinColumn(name = "padre_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private MenuItemVO padre;
    
    @OneToMany(mappedBy = "padre")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<MenuItemVO> subitems = new HashSet<>();
    
    private Integer orden;
    
    @Column(nullable = false)
    private Boolean activo = true;
    
    @ManyToMany
    @JoinTable(
        name = "rol_menu",
        joinColumns = @JoinColumn(name = "menu_item_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<RolVO> roles = new HashSet<>();
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MenuItemVO)) return false;
        MenuItemVO that = (MenuItemVO) o;
        return id != null && id.equals(that.getId());
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 31;
    }
}
