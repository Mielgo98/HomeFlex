package com.example.demo.email.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

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
     * Envía un email de activación de cuenta.
     */
    public void enviarEmailActivacion(String destinatario, String nombre, String token) throws MessagingException {
        // Preparar el contexto para la plantilla
        Context context = new Context();
        context.setVariable("nombre", nombre);
        context.setVariable("token", token);
        context.setVariable("activationUrl", appUrl + "/activar?token=" + token);
        
        // Procesar la plantilla
        String contenido = templateEngine.process("emails/activacion-cuenta", context);
        
        // Crear el mensaje
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(emailFrom);
        helper.setTo(destinatario);
        helper.setSubject("Activación de tu cuenta en HomeFlex");
        helper.setText(contenido, true); // true = es HTML
        
        // Enviar el email
        mailSender.send(message);
        
        System.out.println("Email de activación enviado a: " + destinatario);
    }
    
    
    /**
     * Envía un email de recordatorio para la activación de cuenta.
     */
    public void enviarEmailRecordatorio(String destinatario, String nombre, String token, LocalDateTime expiracion) 
            throws MessagingException {
        // Preparar el contexto para la plantilla
        Context context = new Context();
        context.setVariable("nombre", nombre);
        context.setVariable("token", token);
        context.setVariable("activationUrl", appUrl + "/activar?token=" + token);
        
        // Formatear la fecha de expiración
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        context.setVariable("expiracion", expiracion.format(formatter));
        
        // Procesar la plantilla
        String contenido = templateEngine.process("emails/recordatorio-activacion", context);
        
        // Crear el mensaje
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
        
        helper.setFrom(emailFrom);
        helper.setTo(destinatario);
        helper.setSubject("Recordatorio: Activa tu cuenta en HomeFlex");
        helper.setText(contenido, true); // true = es HTML
        
        // Enviar el email
        mailSender.send(message);
        
        System.out.println("Email de recordatorio enviado a: " + destinatario);
    }
    

/**
 * Envía un email informando al usuario que su cuenta ha sido eliminada por no activarla a tiempo.
 */
public void enviarEmailCuentaEliminada(String destinatario, String nombre) throws MessagingException {
    // Preparar el contexto para la plantilla
    Context context = new Context();
    context.setVariable("nombre", nombre);
    
    // Procesar la plantilla
    String contenido = templateEngine.process("emails/cuenta-eliminada", context);
    
    // Crear el mensaje
    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
    
    helper.setFrom(emailFrom);
    helper.setTo(destinatario);
    helper.setSubject("Tu cuenta en HomeFlex ha sido eliminada");
    helper.setText(contenido, true); // true = es HTML
    
    // Enviar el email
    mailSender.send(message);
    
    System.out.println("Email de notificación de eliminación enviado a: " + destinatario);
}
    
    /**
     *  Comprobamos aquellos usuarios que no se han verificado si llegan a la mitad del tiempo de la fecha de verificacion del token para enviarles un recordatorio
     */
    
		@Scheduled(fixedRate = 15000)
		@Transactional
		public void verificarActivacionesPendientes() {
		    LocalDateTime ahora = LocalDateTime.now();
		
		    usuarioRepository.findByVerificadoAndTokenVerificacionIsNotNull(false).stream()
		    .filter(usuario -> usuario.getTokenExpiration() != null 
		            && usuario.getTokenExpiration().isAfter(ahora))
		    .forEach(usuario -> {
		        try {
		            // Calcular punto medio
		            LocalDateTime fechaCreacion = usuario.getFechaRegistro();
		            LocalDateTime fechaExpiracion = usuario.getTokenExpiration();
		            LocalDateTime puntoMedio = fechaCreacion.plus(
		                ChronoUnit.SECONDS.between(fechaCreacion, fechaExpiracion) / 2,
		                ChronoUnit.SECONDS
		            );
		            
		            //Enviamos recordatorio si se ha sobrepasado el punto medio
		            if (ahora.isAfter(puntoMedio) && !usuario.getRecordatorio()) {
		                enviarEmailRecordatorio(
		                    usuario.getEmail(), 
		                    usuario.getNombre(), 
		                    usuario.getTokenVerificacion(),
		                    usuario.getTokenExpiration()
		                );
		                usuario.setRecordatorio(true);
		            }
		        } catch (MessagingException e) {
		            System.err.println("Error al enviar recordatorio a " + usuario.getEmail() + ": " + e.getMessage());
		        }
		    });    	
		}
    
    /**
     * Eliminamos aquellos usuarios de la base de datos  que en el tiempo anterior a la expiracion del token no han verificado la cuenta 
     */
		   @Scheduled(fixedRate = 30000)
		    @Transactional
		    public void eliminarUsuariosTokenExpirado() {
		        LocalDateTime ahora = LocalDateTime.now();
		        usuarioRepository.findByVerificadoAndTokenVerificacionIsNotNull(false).stream()
		        .filter(usuario -> usuario.getTokenExpiration() != null && usuario.getTokenExpiration().isBefore(ahora))
		        .forEach(usuario -> {
		            try {
		                // Enviar correo de notificación de cuenta eliminada
		                enviarEmailCuentaEliminada(
		                    usuario.getEmail(),
		                    usuario.getNombre()
		                );
		                System.out.println("Eliminando usuario con token expirado: " + usuario.getEmail());
		                usuarioRepository.delete(usuario);
		            } catch (MessagingException e) {
		                System.err.println("Error al enviar notificación de eliminación a " + usuario.getEmail() + ": " + e.getMessage());
		                // Eliminamos el usuario de todas formas
		                usuarioRepository.delete(usuario);
		            }
		        });
		    }
}