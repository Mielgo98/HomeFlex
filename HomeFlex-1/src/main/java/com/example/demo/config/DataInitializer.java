package com.example.demo.config;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.chatbot.repository.DocumentEmbeddingRepository;
import com.example.demo.chatbot.service.EmbeddingService;
import com.example.demo.propiedad.repository.PropiedadRepository;
import com.example.demo.rol.model.RolVO;
import com.example.demo.rol.repository.RolRepository;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.repository.UsuarioRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private RolRepository rolRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PropiedadRepository propiedadRepository;
    
    @Autowired
    private EmbeddingService embeddingService;
    
    @Autowired
    private DocumentEmbeddingRepository embeddingRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("==================================================");
        System.out.println("INICIALIZANDO DATOS DE LA APLICACIÓN");
        System.out.println("==================================================");
        
        // 1) Roles
        createRoleIfNotExists("INQUILINO");
        createRoleIfNotExists("PROPIETARIO");
        createRoleIfNotExists("ADMIN");
        createRoleIfNotExists("SUPER_ADMIN");
        verifyRoles();
        
        // 2) Super-admin
        createSuperAdminUserIfNotExists();
        
        // 3) Embeddings: sólo si no hay ninguno aún
        long existing = embeddingRepository.count();
        if (existing == 0 && propiedadRepository.countByActivoTrue() > 0) {
            System.out.println("▶ La tabla document_embeddings está vacía. Generando embeddings para todas las propiedades activas...");
            embeddingService.updateAllPropertyEmbeddings();
            System.out.println("✅ Embeddings generados y guardados en la base de datos");
        } else {
            System.out.printf("ℹ️ Ya existen %d embeddings. No se recrearán.%n", existing);
        }

        System.out.println("==================================================");
        System.out.println("INICIALIZACIÓN DE DATOS COMPLETADA");
        System.out.println("==================================================");
    }
    
    private void createRoleIfNotExists(String roleName) {
        if (rolRepository.findByNombre(roleName).isEmpty()) {
            try {
                RolVO rol = new RolVO();
                rol.setNombre(roleName);
                rolRepository.save(rol);
                entityManager.flush();
                System.out.println("✅ Rol '" + roleName + "' creado exitosamente");
            } catch (Exception e) {
                System.err.println("❌ Error al crear rol '" + roleName + "': " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("ℹ️ Rol '" + roleName + "' ya existe en la base de datos");
        }
    }
    
    private void verifyRoles() {
        System.out.println("--------------------------------------------------");
        System.out.println("VERIFICACIÓN DE ROLES");
        System.out.println("--------------------------------------------------");
        
        long count = rolRepository.count();
        System.out.println("Total de roles en la base de datos: " + count);
        rolRepository.findAll().forEach(rol ->
            System.out.println("- Rol encontrado: " + rol.getNombre() + " (ID: " + rol.getId() + ")")
        );
        System.out.println("--------------------------------------------------");
    }
    
    @Transactional
    private void createSuperAdminUserIfNotExists() {
        System.out.println("--------------------------------------------------");
        System.out.println("CREACIÓN DE USUARIO SUPER ADMINISTRADOR");
        System.out.println("--------------------------------------------------");
        
        Optional<UsuarioVO> existing = usuarioRepository.findUserEntityByEmail("superadmin@homeflex.com");
        if (existing.isEmpty()) {
            try {
                RolVO rolSuperAdmin = rolRepository.findByNombre("SUPER_ADMIN")
                    .orElseThrow(() -> new RuntimeException("El rol SUPER_ADMIN no se encuentra"));

                UsuarioVO superAdmin = new UsuarioVO();
                superAdmin.setUsername("superAdmin");
                superAdmin.setEmail("superadmin@homeflex.com");
                superAdmin.setPassword(passwordEncoder.encode("SuperAdmin2025"));
                superAdmin.setNombre("Super");
                superAdmin.setApellidos("Administrador");
                superAdmin.setTelefono("666000111");
                superAdmin.setFechaRegistro(LocalDateTime.now());
                superAdmin.setVerificado(true);
                superAdmin.setIsEnabled(true);
                superAdmin.setAccountNonExpired(true);
                superAdmin.setAccountNonLocked(true);
                superAdmin.setCredentialsNonExpired(true);
                Set<RolVO> roles = new HashSet<>();
                roles.add(rolSuperAdmin);
                superAdmin.setRoles(roles);
                
                usuarioRepository.save(superAdmin);
                entityManager.flush();

                System.out.println("✅ Usuario super administrador creado exitosamente");
                System.out.println("   Email: superadmin@homeflex.com");
                System.out.println("   Contraseña: SuperAdmin2025");
            } catch (Exception e) {
                System.err.println("❌ Error al crear super-admin: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("ℹ️ El usuario super administrador ya existe en la base de datos");
        }
        
        System.out.println("--------------------------------------------------");
    }
}
