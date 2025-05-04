package com.example.demo.usuario.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.email.service.EmailService;
import com.example.demo.foto.model.FotoVO;
import com.example.demo.foto.repository.FotoRepository;
import com.example.demo.propiedad.model.PropiedadDTO;
import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.repository.PropiedadRepository;
import com.example.demo.reserva.model.ReservaVO;
import com.example.demo.reserva.repository.ReservaRepository;
import com.example.demo.rol.model.RolVO;
import com.example.demo.rol.repository.RolRepository;
import com.example.demo.usuario.model.PerfilDTO;
import com.example.demo.usuario.model.RegistroDTO;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.repository.UsuarioRepository;
import com.example.demo.valoracion.model.ValoracionVO;
import com.example.demo.valoracion.repository.ValoracionRepository;

import jakarta.mail.MessagingException;
import jakarta.persistence.EntityManager;

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
    
    @Autowired
    private ValoracionRepository valoracionRepository;

    @Autowired
    private PropiedadRepository propiedadRepository;
    
    @Autowired
    private FotoRepository fotoRepository;
    
    @Autowired
    private ReservaRepository reservaRepository;
    
    @Autowired
    private EntityManager entityManager;
    
    private UsuarioVO convertToUsuarioVO(RegistroDTO dto) {
        UsuarioVO usuario = new UsuarioVO();
        usuario.setUsername(dto.getUsername());
        usuario.setEmail(dto.getEmail());
        usuario.setPassword(passwordEncoder.encode(dto.getPassword()));
        usuario.setNombre(dto.getNombre());
        usuario.setApellidos(dto.getApellidos());
        usuario.setTelefono(dto.getTelefono());
        usuario.setFechaRegistro(LocalDateTime.now());
        
        usuario.setVerificado(false);
        usuario.setRecordatorio(false);
        usuario.setIsEnabled(false);
        usuario.setAccountNonExpired(true);
        usuario.setAccountNonLocked(true);
        usuario.setCredentialsNonExpired(true);
        
        String token = UUID.randomUUID().toString();
        usuario.setTokenVerificacion(token);
        usuario.setTokenExpiration(LocalDateTime.now().plusMinutes(3));
        
        return usuario;
    }
    
    @Transactional
    public void registroUsuario(RegistroDTO registro) {
        if (usuarioRepository.findByUsername(registro.getUsername()).isPresent())
            throw new RuntimeException("El nombre de usuario ya está en uso");
        if (usuarioRepository.findUserEntityByEmail(registro.getEmail()).isPresent())
            throw new RuntimeException("El email ya está registrado");
        
        UsuarioVO user = convertToUsuarioVO(registro);
        RolVO rolInquilino = rolRepository.findByNombre("INQUILINO")
                .orElseThrow(() -> new RuntimeException("Rol INQUILINO no encontrado"));
        user.addRol(rolInquilino);
        
        UsuarioVO savedUser = usuarioRepository.save(user);
        try {
            emailService.enviarEmailActivacion(
                savedUser.getEmail(), 
                savedUser.getNombre(), 
                savedUser.getTokenVerificacion()
            );
        } catch (MessagingException e) {
            System.err.println("Error al enviar email de activación: " + e.getMessage());
        }
    }
    
    @Transactional
    public boolean activarCuenta(String token) {
        try {
            UsuarioVO usuario = usuarioRepository.findByTokenVerificacion(token)
                .orElseThrow(() -> new RuntimeException("Token inválido"));
            if (usuario.getTokenExpiration().isBefore(LocalDateTime.now()))
                throw new RuntimeException("Token expirado");
            usuario.setVerificado(true);
            usuario.setIsEnabled(true);
            usuario.setTokenVerificacion(null);
            usuario.setTokenExpiration(null);
            usuarioRepository.save(usuario);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    
    public UsuarioVO buscarPorById(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
    
    public UsuarioVO buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @Transactional
    public UsuarioVO actualizarPerfil(PerfilDTO perfilDTO) {
        UsuarioVO usuario = usuarioRepository.findById(perfilDTO.getId())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!usuario.getUsername().equals(auth.getName()))
            throw new RuntimeException("No tienes permiso para modificar este perfil");
        usuario.setNombre(perfilDTO.getNombre());
        usuario.setApellidos(perfilDTO.getApellidos());
        usuario.setTelefono(perfilDTO.getTelefono());
        if (perfilDTO.getFotoPerfil() != null && !perfilDTO.getFotoPerfil().isEmpty())
            usuario.setFotoPerfil(perfilDTO.getFotoPerfil());
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void cambiarPassword(String passwordActual, String passwordNueva) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UsuarioVO usuario = usuarioRepository.findByUsername(auth.getName())
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (!passwordEncoder.matches(passwordActual, usuario.getPassword()))
            throw new RuntimeException("Contraseña actual incorrecta");
        if (passwordEncoder.matches(passwordNueva, usuario.getPassword()))
            throw new RuntimeException("La nueva contraseña debe ser diferente");
        usuario.setPassword(passwordEncoder.encode(passwordNueva));
        usuarioRepository.save(usuario);
    }
    
    @Transactional
    public boolean darDeBajaUsuario(String password) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth.getName();
            
            UsuarioVO usuario = usuarioRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
            
            if (!passwordEncoder.matches(password, usuario.getPassword())) {
                throw new RuntimeException("Contraseña incorrecta");
            }
            
            // 1. Eliminar todas las propiedades del usuario
            List<PropiedadVO> propiedades = propiedadRepository.findByPropietario(usuario);
            for (PropiedadVO propiedad : propiedades) {
                List<FotoVO> fotos = fotoRepository.findByPropiedad(propiedad);
                fotoRepository.deleteAll(fotos);
                
                List<ValoracionVO> valoracionesPropiedad = valoracionRepository.findByPropiedad(propiedad);
                valoracionRepository.deleteAll(valoracionesPropiedad);
                
                List<ReservaVO> reservasPropiedad = reservaRepository.findByPropiedad(propiedad);
                reservaRepository.deleteAll(reservasPropiedad);
                
                propiedadRepository.delete(propiedad);
            }
            
            // 2. Eliminar todas las reservas del usuario
            List<ReservaVO> reservasUsuario = reservaRepository.findByUsuario(usuario);
            reservaRepository.deleteAll(reservasUsuario);
            
            // 3. Eliminar todas las valoraciones del usuario
            List<ValoracionVO> valoracionesUsuario = valoracionRepository.findByUsuario(usuario);
            valoracionRepository.deleteAll(valoracionesUsuario);
            
            // Eliminar roles usando consulta nativa
            entityManager.createNativeQuery("DELETE FROM usuarios_roles WHERE usuario_id = :userId")
                    .setParameter("userId", usuario.getId())
                    .executeUpdate();
            
            // 4. IMPORTANTE: Limpiar explícitamente los roles
            usuario.getRoles().clear();
            usuarioRepository.saveAndFlush(usuario);
            
            // 5. Eliminar el usuario
            usuarioRepository.delete(usuario);
            
            // 6. Limpiar contexto de seguridad
            SecurityContextHolder.clearContext();
            
            return true;
            
        } catch (Exception e) {
            throw new RuntimeException("Error al dar de baja al usuario: " + e.getMessage());
        }
    }
    
    public Map<String, Long> contarUsuariosPorRol() {
        List<Object[]> resultados = usuarioRepository.contarUsuariosPorRol();
        Map<String, Long> mapa = new HashMap<>();
        for (Object[] fila : resultados)
            mapa.put((String)fila[0], ((Number)fila[1]).longValue());
        return mapa;
    }
    
    public List<PropiedadDTO> obtenerPropiedadesFavoritas(String username) {
        UsuarioVO usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return usuario.getPropiedadesFavoritas().stream()
                .map(PropiedadDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void agregarPropiedadFavorita(String username, Long propiedadId) {
        UsuarioVO usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        PropiedadVO propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
        if (!usuario.getPropiedadesFavoritas().contains(propiedad)) {
            usuario.getPropiedadesFavoritas().add(propiedad);
            usuarioRepository.save(usuario);
        }
    }

    @Transactional
    public void eliminarPropiedadFavorita(String username, Long propiedadId) {
        UsuarioVO usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        PropiedadVO propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
        usuario.getPropiedadesFavoritas().remove(propiedad);
        usuarioRepository.save(usuario);
    }
    
    /**
     * Registra un ADMINISTRADOR (directamente verificado y habilitado).
     */
    @Transactional
    public void registrarAdministrador(RegistroDTO dto) {
        // Validación de contraseñas
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("Las contraseñas no coinciden");
        }
        if (usuarioRepository.findByUsername(dto.getUsername()).isPresent())
            throw new RuntimeException("El username ya existe");
        if (usuarioRepository.findUserEntityByEmail(dto.getEmail()).isPresent())
            throw new RuntimeException("El email ya está en uso");

        UsuarioVO admin = convertToUsuarioVO(dto);
        // Aquí buscamos "ADMIN", que es el nombre que tienes en tu BD
        RolVO rolAdmin = rolRepository.findByNombre("ADMIN")
            .orElseThrow(() -> new RuntimeException("Rol ADMIN no encontrado"));
        admin.addRol(rolAdmin);
        admin.setVerificado(true);
        admin.setIsEnabled(true);
        usuarioRepository.save(admin);
    }

    public List<UsuarioVO> obtenerAdministradores() {
        return usuarioRepository.findAllByRolesNombre("ADMIN");
    }
    
    public List<UsuarioVO> obtenerUsuariosNormales() {
        return usuarioRepository.findAllByRolesNombre("ADMIN");
    }

    @Transactional
    public void eliminarUsuario(Long id) {
        usuarioRepository.deleteById(id);
    }

	 /**
     * Recupera todos los usuarios que sean PROPIETARIO o INQUILINO.
     */
    public List<UsuarioVO> buscarPropietariosEInquilinos() {
        List<String> roles = List.of("PROPIETARIO", "INQUILINO");
        return usuarioRepository.findDistinctByRolNombreIn(roles);
    }
    
    /**
     * eliminar usuario por username
     * @param username
     */

    @Transactional
    public void eliminarUsuarioPorUsername(String username) {
        UsuarioVO usuario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        // eliminar datos relacionados como en darDeBajaUsuario...
        propiedadRepository.findByPropietario(usuario).forEach(prop -> {
            fotoRepository.deleteAll(fotoRepository.findByPropiedad(prop));
            valoracionRepository.deleteAll(valoracionRepository.findByPropiedad(prop));
            reservaRepository.deleteAll(reservaRepository.findByPropiedad(prop));
            propiedadRepository.delete(prop);
        });
        reservaRepository.deleteAll(reservaRepository.findByUsuario(usuario));
        valoracionRepository.deleteAll(valoracionRepository.findByUsuario(usuario));
        usuario.getRoles().clear();
        usuarioRepository.delete(usuario);
    }
}
