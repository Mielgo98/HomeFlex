package com.example.demo.valoracion.model;

import java.time.LocalDateTime;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para recibir la creación de una valoración
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValoracionDTO {
    
    private Long id;
    private Long propiedadId;
    private String propiedadTitulo;
    private String propiedadImagen;
    private Long usuarioId;
    private String usuarioNombre;
    private Integer puntuacion;
    private String comentario;
    private LocalDateTime fechaCreacion;
    private Boolean aprobada;
    private Integer limpieza;
    private Integer ubicacion;
    private Integer comunicacion;
    private Integer calidad;
    private String respuestaPropietario;
    private LocalDateTime fechaRespuesta;
    
    /**
     * Constructor a partir de la entidad ValoracionVO
     */
    public ValoracionDTO(ValoracionVO vo) {
        this.id = vo.getId();
        this.propiedadId = vo.getPropiedad().getId();
        this.propiedadTitulo = vo.getPropiedad().getTitulo();
        this.propiedadImagen = vo.getPropiedad().getFotoPrincipal();
        this.usuarioId = vo.getUsuario().getId();
        this.usuarioNombre = vo.getUsuario().getNombre() + " " + vo.getUsuario().getApellidos();
        this.puntuacion = vo.getPuntuacion();
        this.comentario = vo.getComentario();
        this.fechaCreacion = vo.getFechaCreacion();
        this.aprobada = vo.getAprobada();
        this.limpieza = vo.getLimpieza();
        this.ubicacion = vo.getUbicacion();
        this.comunicacion = vo.getComunicacion();
        this.calidad = vo.getCalidad();
        this.respuestaPropietario = vo.getRespuestaPropietario();
        this.fechaRespuesta = vo.getFechaRespuesta();
    }
}



