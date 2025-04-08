package com.example.demo.service.usuario;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.model.rol.RolVO;
import com.example.demo.model.usuario.RegistroDTO;
import com.example.demo.model.usuario.UsuarioVO;
import com.example.demo.repository.rol.RolRepository;
import com.example.demo.repository.usuario.UsuarioRepository;
import com.example.demo.service.email.EmailService;

import jakarta.mail.MessagingException;

@Service
public class UsuarioService {

    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private RolRepository rolRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private EmailService emailService;

    /**
     * Convierte el DTO de registro a un objeto UsuarioVO, asignándole los valores
     * iniciales y, por defecto, el rol "INQUILINO".
     */
    private UsuarioVO convertToUsuarioVO(RegistroDTO dto) {
        UsuarioVO usuario = new UsuarioVO();
        usuario.setEmail(dto.getEmail());
        // Se codifica la contraseña usando el PasswordEncoder
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setNombre(dto.getNombre());
        usuario.setApellidos(dto.getApellidos());
        usuario.setTelefono(dto.getTelefono());
        usuario.setFechaRegistro(LocalDateTime.now());
        
        // Configuración inicial de la cuenta - Deshabilitada hasta activación
        usuario.setVerificado(false);
        usuario.setIsEnabled(false); // La cuenta estará deshabilitada hasta que se active
        usuario.setAccountNonExpired(true);
        usuario.setAccountNonLocked(true);
        usuario.setCredentialsNonExpired(true);
        
        // Genera un token de verificación y define su expiración (24 horas)
        String token = UUID.randomUUID().toString();
        usuario.setTokenVerificacion(token);
        usuario.setTokenExpiration(LocalDateTime.now().plusHours(24));
        
        return usuario;
    }
    
    /**
     * Registra un nuevo usuario convirtiendo el RegistroDTO a UsuarioVO y guardándolo en la base de datos.
     * Además, envía un email de activación.
     */
    @Transactional
    public void registroUsuario(RegistroDTO registro) {
        try {
            // Crear el usuario con los datos básicos
            UsuarioVO user = convertToUsuarioVO(registro);
            
            // Buscar el rol INQUILINO
            RolVO rolInquilino = rolRepository.findByNombre("INQUILINO")
                    .orElseThrow(() -> new RuntimeException("El rol INQUILINO no se encuentra en la base de datos"));
            
            // Asignar el rol al usuario
            user.addRol(rolInquilino);
            
            // Guardar el usuario
            UsuarioVO savedUser = usuarioRepository.save(user);
            
            System.out.println("Usuario registrado con éxito: " + savedUser.getEmail());
            System.out.println("Roles asignados: ");
            savedUser.getRoles().forEach(rol -> System.out.println("- " + rol.getNombre()));
            
            // Enviar email de activación
            try {
                emailService.enviarEmailActivacion(
                    savedUser.getEmail(), 
                    savedUser.getNombre(), 
                    savedUser.getTokenVerificacion()
                );
            } catch (MessagingException e) {
                System.err.println("Error al enviar email de activación: " + e.getMessage());
                // Continuamos con el proceso aunque falle el envío de email
            }
            
        } catch (Exception e) {
            System.err.println("Error al registrar usuario: " + e.getMessage());
            e.printStackTrace();
            throw e; // Re-lanzamos la excepción para manejarla en el controlador
        }
    }
    
    /**
     * Activa la cuenta de un usuario utilizando el token proporcionado.
     * @param token El token de verificación.
     * @return true si la activación fue exitosa, false si no se encontró el token o ha expirado.
     */
    @Transactional
    public boolean activarCuenta(String token) {
        try {
            // Buscar usuario por token
            UsuarioVO usuario = usuarioRepository.findByTokenVerificacion(token)
                    .orElseThrow(() -> new RuntimeException("Token de verificación no válido"));
            
            // Verificar que el token no haya expirado
            if (usuario.getTokenExpiration().isBefore(LocalDateTime.now())) {
                throw new RuntimeException("El token de verificación ha expirado");
            }
            
            // Activar la cuenta
            usuario.setVerificado(true);
            usuario.setIsEnabled(true);
            usuario.setTokenVerificacion(null); // Eliminamos el token una vez usado
            usuario.setTokenExpiration(null);
            
            usuarioRepository.save(usuario);
            
            System.out.println("Cuenta activada con éxito para: " + usuario.getEmail());
            return true;
            
        } catch (Exception e) {
            System.err.println("Error al activar cuenta: " + e.getMessage());
            return false;
        }
    }
}