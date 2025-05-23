package com.example.demo.model.usuario;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import com.example.demo.model.rol.RolVO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuario")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioVO {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 100)
    private String email;
    
    @Column(nullable = false, length = 255)
    private String password;
    
    @Column(length = 50, nullable = false)
    private String nombre;
    
    @Column(length = 100, nullable = false)
    private String apellidos;
    
    @Column(length = 20, unique = true, nullable = false)
    private String telefono;
    
    // Relación many-to-many con la entidad Rol - Aseguramos que se cargue EAGER
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "usuario_roles",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    private Set<RolVO> roles = new HashSet<>();
    
    @Column(name = "foto_perfil", length = 255)
    private String fotoPerfil;
    
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
    
    @Column(nullable = false)
    private Boolean verificado;
    
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled;
    
    @Column(name = "account_non_expired", nullable = false)
    private Boolean accountNonExpired;
    
    @Column(name = "account_non_locked", nullable = false)
    private Boolean accountNonLocked;
    
    @Column(name = "credentials_non_expired", nullable = false)
    private Boolean credentialsNonExpired;
    
    @Column(name = "token_verificacion", length = 100)
    private String tokenVerificacion;
    
    @Column(name = "token_expiration")
    private LocalDateTime tokenExpiration;
    
    // Método helper para añadir un rol
    public void addRol(RolVO rol) {
        if (this.roles == null) {
            this.roles = new HashSet<>();
        }
        this.roles.add(rol);
    }
}