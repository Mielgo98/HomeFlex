package com.example.demo.mensaje.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.usuario.model.UsuarioVO;

@Entity
@Table(name = "mensajes")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MensajeVO {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "emisor_id", nullable = false)
    private UsuarioVO emisor;
    
    @ManyToOne
    @JoinColumn(name = "receptor_id", nullable = false)
    private UsuarioVO receptor;
    
    @Column(name = "contenido", nullable = false, columnDefinition = "TEXT")
    private String contenido;
    
    @Column(name = "fecha_envio", nullable = false)
    private LocalDateTime fechaEnvio;
    
    @Column(name = "leido", nullable = false)
    private Boolean leido;
    
    @ManyToOne
    @JoinColumn(name = "propiedad_id")
    private PropiedadVO propiedad;
    
    @Column(name = "tipo_mensaje", nullable = false)
    private String tipoMensaje = "texto"; // "texto", "imagen", etc.
    
    @Column(name = "url_recurso")
    private String urlRecurso; // Para almacenar URLs de imágenes u otros recursos
}