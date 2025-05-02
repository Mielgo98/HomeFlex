package com.example.demo.mensaje.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversacionDTO {
    private Long contactoId;
    private String contactoNombre;
    private String contactoApellidos;
    private String contactoFoto;
    private String ultimoMensaje;
    private String tipoUltimoMensaje;
    private LocalDateTime fechaUltimoMensaje;
    private Long propiedadId;
    private String propiedadTitulo;
    private Integer mensajesNoLeidos;
}