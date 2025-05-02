package com.example.demo.mensaje.model;



import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MensajeDTO {
    private Long id;
    private Long emisorId;
    private String emisorNombre;
    private String emisorApellidos;
    private String emisorFoto;
    private Long receptorId;
    private String receptorNombre;
    private String receptorApellidos;
    private String receptorFoto;
    private String contenido;
    private LocalDateTime fechaEnvio;
    private Boolean leido;
    private Long propiedadId;
    private String propiedadTitulo;
    private String tipoMensaje;
    private String urlRecurso;
}