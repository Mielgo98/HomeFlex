package com.example.demo.service.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private TemplateEngine templateEngine;
    
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
}