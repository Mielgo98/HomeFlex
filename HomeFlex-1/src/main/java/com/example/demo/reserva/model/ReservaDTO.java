package com.example.demo.reserva.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para transferir información de reservas entre capas
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaDTO {
    
    private Long id;
    private Long propiedadId;
    private String tituloPropiedad;
    private String fotoPropiedad;
    private String ciudadPropiedad;
    private String paisPropiedad;
    private Long usuarioId;
    private String nombreUsuario;
    private String apellidosUsuario;
    private LocalDate fechaInicio;
    private LocalDate fechaFin;
    private Integer numHuespedes;
    private BigDecimal precioTotal;
    private EstadoReserva estado;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaConfirmacion;
    private String codigoReserva;
    private String comentarios;
    
    // Constructor a partir de la entidad
    public ReservaDTO(ReservaVO reserva) {
        this.id = reserva.getId();
        this.propiedadId = reserva.getPropiedad().getId();
        this.tituloPropiedad = reserva.getPropiedad().getTitulo();
        this.fotoPropiedad = reserva.getPropiedad().getFotoPrincipal();
        this.ciudadPropiedad = reserva.getPropiedad().getCiudad();
        this.paisPropiedad = reserva.getPropiedad().getPais();
        this.usuarioId = reserva.getUsuario().getId();
        this.nombreUsuario = reserva.getUsuario().getNombre();
        this.apellidosUsuario = reserva.getUsuario().getApellidos();
        this.fechaInicio = reserva.getFechaInicio();
        this.fechaFin = reserva.getFechaFin();
        this.numHuespedes = reserva.getNumHuespedes();
        this.precioTotal = reserva.getPrecioTotal();
        this.estado = reserva.getEstado();
        this.fechaSolicitud = reserva.getFechaSolicitud();
        this.fechaConfirmacion = reserva.getFechaConfirmacion();
        this.codigoReserva = reserva.getCodigoReserva();
        this.comentarios = reserva.getComentarios();
    }
}