package com.example.demo.usuario.service;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.email.service.EmailService;
import com.example.demo.rol.model.RolVO;
import com.example.demo.rol.repository.RolRepository;
import com.example.demo.usuario.model.PerfilDTO;
import com.example.demo.usuario.model.RegistroDTO;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.repository.UsuarioRepository;

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
        usuario.setUsername(dto.getUsername());
        usuario.setEmail(dto.getEmail());
        // Se codifica la contraseña usando el PasswordEncoder
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setNombre(dto.getNombre());
        usuario.setApellidos(dto.getApellidos());
        usuario.setTelefono(dto.getTelefono());
        usuario.setFechaRegistro(LocalDateTime.now());
        
        // Configuración inicial de la cuenta - Deshabilitada hasta activación
        usuario.setVerificado(false);
        usuario.setRecordatorio(false);
        usuario.setIsEnabled(false); // La cuenta estará deshabilitada hasta que se active
        usuario.setAccountNonExpired(true);
        usuario.setAccountNonLocked(true);
        usuario.setCredentialsNonExpired(true);
        
        // Genera un token de verificación y define su expiración (24 horas)
        String token = UUID.randomUUID().toString();
        usuario.setTokenVerificacion(token);
        usuario.setTokenExpiration(LocalDateTime.now().plusMinutes(3));
     // Justo después de asignar los tokens
        System.out.println("Token generado: " + token);
        System.out.println("Verificación de token asignado: " + usuario.getTokenVerificacion());
        System.out.println("Expiración establecida: " + usuario.getTokenExpiration());
        return usuario;
    }
    
    /**
     * Registra un nuevo usuario convirtiendo el RegistroDTO a UsuarioVO y guardándolo en la base de datos.
     * Además, envía un email de activación.
     */
    @Transactional
    public void registroUsuario(RegistroDTO registro) {
        try {
            // Verificar si el username ya existe
            if (usuarioRepository.findByUsername(registro.getUsername()).isPresent()) {
                throw new RuntimeException("El nombre de usuario ya está en uso");
            }
            
            // Verificar si el email ya existe
            if (usuarioRepository.findUserEntityByEmail(registro.getEmail()).isPresent()) {
                throw new RuntimeException("El email ya está registrado");
            }
            
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
    
    /**
     * Busca un usuario por su nombre de usuario
     * @param username Nombre de usuario a buscar
     * @return El objeto UsuarioVO si se encuentra
     * @throws RuntimeException si el usuario no existe
     */
    public UsuarioVO buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    /**
     * Actualiza la información del perfil de un usuario
     * @param perfilDTO Datos del perfil a actualizar
     * @return El usuario actualizado
     * @throws RuntimeException si hay errores durante la actualización
     */
    @Transactional
    public UsuarioVO actualizarPerfil(PerfilDTO perfilDTO) {
        try {
            // Buscar el usuario por ID
            UsuarioVO usuario = usuarioRepository.findById(perfilDTO.getId())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Verificar que solo el propio usuario pueda modificar su perfil
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String usernameActual = auth.getName();
            
            if (!usuario.getUsername().equals(usernameActual)) {
                throw new RuntimeException("No tienes permiso para modificar este perfil");
            }
            
            // Actualizar solo los campos permitidos
            usuario.setNombre(perfilDTO.getNombre());
            usuario.setApellidos(perfilDTO.getApellidos());
            usuario.setTelefono(perfilDTO.getTelefono());
            
            // Actualizar foto de perfil si se proporciona
            if (perfilDTO.getFotoPerfil() != null && !perfilDTO.getFotoPerfil().isEmpty()) {
                usuario.setFotoPerfil(perfilDTO.getFotoPerfil());
            }
            
            // Guardar los cambios
            return usuarioRepository.save(usuario);
            
        } catch (Exception e) {
            throw new RuntimeException("Error al actualizar el perfil: " + e.getMessage());
        }
    }

    
    /**
     * Cambia la contraseña de un usuario verificando primero la contraseña actual
     * @param passwordActual Contraseña actual del usuario
     * @param passwordNueva Nueva contraseña
     * @throws RuntimeException si la contraseña actual no es correcta o hay otro error
     */
    @Transactional
    public void cambiarPassword(String passwordActual, String passwordNueva) {
        try {
            // Obtener el usuario autenticado
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            // Buscar el usuario en la base de datos
            UsuarioVO usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Verificar que la contraseña actual sea correcta
            if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
                throw new RuntimeException("La contraseña actual no es correcta");
            }
            
            // Verificar que la nueva contraseña no sea igual a la actual
            if (passwordEncoder.matches(passwordNueva, usuario.getPassword())) {
                throw new RuntimeException("La nueva contraseña debe ser diferente a la actual");
            }
            
            // Codificar y establecer la nueva contraseña
            String encodedPassword = passwordEncoder.encode(passwordNueva);
            usuario.setPassword(encodedPassword);
            
            // Guardar los cambios
            usuarioRepository.save(usuario);
            
            System.out.println("Contraseña actualizada correctamente para el usuario: " + username);
            
        } catch (Exception e) {
            throw new RuntimeException("Error al cambiar la contraseña: " + e.getMessage());
        }
    }
    
    /**
     * Da de baja a un usuario del sistema verificando su contraseña primero
     * @param password Contraseña del usuario para confirmar la operación
     * @return true si se ha eliminado con éxito
     * @throws RuntimeException si la contraseña no es correcta o hay otro error
     */
    @Transactional
    public boolean darDeBajaUsuario(String password) {
        try {
            // Obtener el usuario autenticado
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            // Buscar el usuario en la base de datos
            UsuarioVO usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            // Verificar que la contraseña sea correcta
            if (!passwordEncoder.matches(password, usuario.getPassword())) {
                throw new RuntimeException("La contraseña proporcionada no es correcta");
            }
            
            // Eliminar el usuario
            usuarioRepository.delete(usuario);
            
            // Cerrar la sesión del usuario
            SecurityContextHolder.clearContext();
            
            System.out.println("Usuario eliminado correctamente: " + username);
            return true;
            
        } catch (Exception e) {
            throw new RuntimeException("Error al dar de baja al usuario: " + e.getMessage());
        }
    }
}