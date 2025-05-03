package com.example.demo.email.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.repository.UsuarioRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.transaction.Transactional;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private TemplateEngine templateEngine;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Value("${spring.mail.username}") 
    private String emailFrom;
    
    @Value("${app.url}")
    private String appUrl;
    
    /**
     * Método genérico para enviar cualquier plantilla Thymeleaf.
     * @param destinatario dirección del receptor
     * @param asunto asunto del correo
     * @param plantilla nombre de la plantilla (en src/main/resources/templates/emails/, sin .html)
     * @param variables mapa con las variables que la plantilla necesita
     */
    public void send(
            String destinatario,
            String asunto,
            String plantilla,
            Map<String, Object> variables
    ) throws MessagingException {
        Context ctx = new Context();
        variables.forEach(ctx::setVariable);
        String htmlBody = templateEngine.process("emails/" + plantilla, ctx);

        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
        helper.setFrom(emailFrom);
        helper.setTo(destinatario);
        helper.setSubject(asunto);
        helper.setText(htmlBody, true);

        mailSender.send(msg);
        System.out.println("Email '" + asunto + "' enviado a: " + destinatario);
    }
    
    /** Envía email de activación de cuenta */
    public void enviarEmailActivacion(String destinatario, String nombre, String token) throws MessagingException {
        send(
            destinatario,
            "Activación de tu cuenta en HomeFlex",
            "activacion-cuenta",
            Map.of(
                "nombre", nombre,
                "token", token,
                "activationUrl", appUrl + "/activar?token=" + token
            )
        );
    }
    
    /** Envía recordatorio de activación */
    public void enviarEmailRecordatorio(String destinatario, String nombre, String token, LocalDateTime expiracion) 
            throws MessagingException {
        String exp = expiracion.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
        send(
            destinatario,
            "Recordatorio: Activa tu cuenta en HomeFlex",
            "recordatorio-activacion",
            Map.of(
                "nombre", nombre,
                "token", token,
                "activationUrl", appUrl + "/activar?token=" + token,
                "expiracion", exp
            )
        );
    }
    
    /** Notifica que la cuenta ha sido eliminada */
    public void enviarEmailCuentaEliminada(String destinatario, String nombre) throws MessagingException {
        send(
            destinatario,
            "Tu cuenta en HomeFlex ha sido eliminada",
            "cuenta-eliminada",
            Map.of("nombre", nombre)
        );
    }
    
    /** Recordatorio automático a mitad de expiración */
    @Scheduled(fixedRate = 15000)
    @Transactional
    public void verificarActivacionesPendientes() {
        LocalDateTime ahora = LocalDateTime.now();
        usuarioRepository.findByVerificadoAndTokenVerificacionIsNotNull(false).stream()
            .filter(u -> u.getTokenExpiration() != null && u.getTokenExpiration().isAfter(ahora))
            .forEach(u -> {
                LocalDateTime creado = u.getFechaRegistro();
                LocalDateTime exp = u.getTokenExpiration();
                long segundos = ChronoUnit.SECONDS.between(creado, exp);
                LocalDateTime mitad = creado.plusSeconds(segundos/2);
                if (ahora.isAfter(mitad) && !u.getRecordatorio()) {
                    try {
                        enviarEmailRecordatorio(u.getEmail(), u.getNombre(), u.getTokenVerificacion(), exp);
                        u.setRecordatorio(true);
                    } catch (MessagingException e) {
                        System.err.println("Error recordatorio a " + u.getEmail() + ": " + e.getMessage());
                    }
                }
            });
    }
    
    /** Elimina y notifica cuentas expiradas */
    @Scheduled(fixedRate = 30000)
    @Transactional
    public void eliminarUsuariosTokenExpirado() {
        LocalDateTime ahora = LocalDateTime.now();
        usuarioRepository.findByVerificadoAndTokenVerificacionIsNotNull(false).stream()
            .filter(u -> u.getTokenExpiration() != null && u.getTokenExpiration().isBefore(ahora))
            .forEach(u -> {
                try {
                    enviarEmailCuentaEliminada(u.getEmail(), u.getNombre());
                } catch (MessagingException e) {
                    System.err.println("Error notificación eliminación a " + u.getEmail() + ": " + e.getMessage());
                } finally {
                    usuarioRepository.delete(u);
                    System.out.println("Usuario eliminado: " + u.getEmail());
                }
            });
    }
    
    /** Notifica al propietario nueva valoración */
    public void enviarNotificacionNuevaValoracion(
            String emailPropietario,
            String nombrePropietario,
            String nombreValorador,
            String tituloPropiedad,
            Integer puntuacion,
            String comentario
    ) throws MessagingException {
        send(
            emailPropietario,
            "Nueva valoración para tu propiedad en HomeFlex",
            "nueva-valoracion",
            Map.of(
                "nombrePropietario", nombrePropietario,
                "nombreValorador", nombreValorador,
                "tituloPropiedad", tituloPropiedad,
                "puntuacion", puntuacion,
                "comentario", comentario,
                "appUrl", appUrl
            )
        );
    }
    
    /** Notifica al usuario respuesta a su valoración */
    public void enviarNotificacionRespuestaValoracion(
            String emailUsuario,
            String nombreUsuario,
            String nombrePropietario,
            String tituloPropiedad,
            String respuesta
    ) throws MessagingException {
        send(
            emailUsuario,
            "Han respondido a tu valoración en HomeFlex",
            "respuesta-valoracion",
            Map.of(
                "nombreUsuario", nombreUsuario,
                "nombrePropietario", nombrePropietario,
                "tituloPropiedad", tituloPropiedad,
                "respuesta", respuesta,
                "appUrl", appUrl
            )
        );
    }
    
    /** Notifica cambio de contraseña */
    public void sendPasswordChangeEmail(UsuarioVO usuario) throws Exception {
        send(
            usuario.getEmail(),
            "Tu contraseña en HomeFlex ha sido cambiada",
            "password-change",
            Map.of(
                "nombre", usuario.getNombre(),
                "loginUrl", appUrl + "/login",
                "soporteUrl", appUrl + "/soporte"
            )
        );
    }
    
    /**
     * Para envíos genéricos desde admin.
     */
    public void enviarEmailAdminNotificacion(
            String destinatario,
            String nombreUsuario,
            String asunto,
            String mensajeHtml,
            String accionUrl,
            String textoBoton
    ) throws MessagingException {
        Context ctx = new Context();
        ctx.setVariable("nombre", nombreUsuario);
        ctx.setVariable("mensajeHtml", mensajeHtml);
        ctx.setVariable("accionUrl", accionUrl);
        ctx.setVariable("textoBoton", textoBoton);

        String contenido = templateEngine.process("emails/admin-notificacion", ctx);

        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
        h.setFrom(emailFrom);
        h.setTo(destinatario);
        h.setSubject(asunto);
        h.setText(contenido, true);

        mailSender.send(msg);
    }

    /**
     * Notifica eliminación a petición de admin.
     */
    public void enviarEmailCuentaEliminadaPorAdmin(
            String destinatario,
            String nombreUsuario,
            String motivo
    ) throws MessagingException {
        Context ctx = new Context();
        ctx.setVariable("nombre", nombreUsuario);
        ctx.setVariable("motivo", motivo);

        String contenido = templateEngine.process("emails/admin-eliminacion", ctx);

        MimeMessage msg = mailSender.createMimeMessage();
        MimeMessageHelper h = new MimeMessageHelper(msg, "UTF-8");
        h.setFrom(emailFrom);
        h.setTo(destinatario);
        h.setSubject("Tu cuenta ha sido eliminada");
        h.setText(contenido, true);

        mailSender.send(msg);
    }
}
