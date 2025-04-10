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

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        System.out.println("==================================================");
        System.out.println("INICIALIZANDO DATOS DE LA APLICACIÓN");
        System.out.println("==================================================");
        
        // Inicializa roles si no existen
        createRoleIfNotExists("INQUILINO");
        createRoleIfNotExists("PROPIETARIO");
        createRoleIfNotExists("ADMIN");
        createRoleIfNotExists("SUPER_ADMIN"); // Nuevo rol de super administrador
        
        // Verificamos que los roles se han creado correctamente
        verifyRoles();
        
        // Crear usuario super administrador
        createSuperAdminUserIfNotExists();
        
        // Crear usuario administrador normal
        createAdminUserIfNotExists();
        
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
                entityManager.flush(); // Forzar la escritura en base de datos
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
        
        rolRepository.findAll().forEach(rol -> {
            System.out.println("- Rol encontrado: " + rol.getNombre() + " (ID: " + rol.getId() + ")");
        });
        
        System.out.println("--------------------------------------------------");
    }
    
    @Transactional
    private void createSuperAdminUserIfNotExists() {
        System.out.println("--------------------------------------------------");
        System.out.println("CREACIÓN DE USUARIO SUPER ADMINISTRADOR");
        System.out.println("--------------------------------------------------");
        
        Optional<UsuarioVO> existingSuperAdmin = usuarioRepository.findUserEntityByEmail("superadmin@homeflex.com");
        
        if (existingSuperAdmin.isEmpty()) {
            try {
                // Buscar el rol SUPER_ADMIN
                RolVO rolSuperAdmin = rolRepository.findByNombre("SUPER_ADMIN")
                        .orElseThrow(() -> new RuntimeException("El rol SUPER_ADMIN no se encuentra en la base de datos"));
                
                // Crear el usuario super admin
                UsuarioVO superAdmin = new UsuarioVO();
                superAdmin.setEmail("superadmin@homeflex.com");
                superAdmin.setPassword(passwordEncoder.encode("SuperAdmin2025"));
                superAdmin.setNombre("Super");
                superAdmin.setApellidos("Administrador");
                superAdmin.setTelefono("666000111");
                superAdmin.setFechaRegistro(LocalDateTime.now());
                
                // Estados de la cuenta
                superAdmin.setVerificado(true);
                superAdmin.setIsEnabled(true);
                superAdmin.setAccountNonExpired(true);
                superAdmin.setAccountNonLocked(true);
                superAdmin.setCredentialsNonExpired(true);
                
                // Asignar rol SUPER_ADMIN
                Set<RolVO> roles = new HashSet<>();
                roles.add(rolSuperAdmin);
                superAdmin.setRoles(roles);
                
                // Guardar el usuario
                usuarioRepository.save(superAdmin);
                entityManager.flush();
                
                System.out.println("✅ Usuario super administrador creado exitosamente");
                System.out.println("   Email: superadmin@homeflex.com");
                System.out.println("   Contraseña: SuperAdmin2025");
                
            } catch (Exception e) {
                System.err.println("❌ Error al crear usuario super administrador: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("ℹ️ El usuario super administrador ya existe en la base de datos");
        }
        
        System.out.println("--------------------------------------------------");
    }
    
    @Transactional
    private void createAdminUserIfNotExists() {
        System.out.println("--------------------------------------------------");
        System.out.println("CREACIÓN DE USUARIO ADMINISTRADOR");
        System.out.println("--------------------------------------------------");
        
        Optional<UsuarioVO> existingAdmin = usuarioRepository.findUserEntityByEmail("admin@homeflex.com");
        
        if (existingAdmin.isEmpty()) {
            try {
                // Buscar el rol ADMIN
                RolVO rolAdmin = rolRepository.findByNombre("ADMIN")
                        .orElseThrow(() -> new RuntimeException("El rol ADMIN no se encuentra en la base de datos"));
                
                // Crear el usuario admin
                UsuarioVO admin = new UsuarioVO();
                admin.setEmail("admin@homeflex.com");
                admin.setPassword(passwordEncoder.encode("Admin2025"));
                admin.setNombre("Administrador");
                admin.setApellidos("HomeFlex");
                admin.setTelefono("666111222");
                admin.setFechaRegistro(LocalDateTime.now());
                
                // Estados de la cuenta
                admin.setVerificado(true);
                admin.setIsEnabled(true);
                admin.setAccountNonExpired(true);
                admin.setAccountNonLocked(true);
                admin.setCredentialsNonExpired(true);
                
                // Asignar rol ADMIN
                Set<RolVO> roles = new HashSet<>();
                roles.add(rolAdmin);
                admin.setRoles(roles);
                
                // Guardar el usuario
                usuarioRepository.save(admin);
                entityManager.flush();
                
                System.out.println("✅ Usuario administrador creado exitosamente");
                System.out.println("   Email: admin@homeflex.com");
                System.out.println("   Contraseña: SuperAdmin");
                
            } catch (Exception e) {
                System.err.println("❌ Error al crear usuario administrador: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("ℹ️ El usuario administrador ya existe en la base de datos");
        }
        
        System.out.println("--------------------------------------------------");
    }
}