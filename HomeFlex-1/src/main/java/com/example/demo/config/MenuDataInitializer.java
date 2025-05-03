package com.example.demo.config;

import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.menu.model.MenuItemVO;
import com.example.demo.menu.repository.MenuItemRepository;
import com.example.demo.rol.model.RolVO;
import com.example.demo.rol.repository.RolRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
@Order(3)
public class MenuDataInitializer implements CommandLineRunner {

    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private RolRepository rolRepository;
    @PersistenceContext private EntityManager entityManager;

    @Override
    @Transactional
    public void run(String... args) {
        if (menuItemRepository.count() == 0) {
            initializeMenus();
        }
    }
    
    @Transactional
    private void initializeMenus() {
        RolVO superAdmin = rolRepository.findByNombre("SUPER_ADMIN").orElseThrow();
        RolVO admin      = rolRepository.findByNombre("ADMIN").orElseThrow();
        RolVO propietario= rolRepository.findByNombre("PROPIETARIO").orElseThrow();
        RolVO inquilino  = rolRepository.findByNombre("INQUILINO").orElseThrow();
        
        // Menús Super Admin
        MenuItemVO dash    = crear("Dashboard", "/admin/dashboard", "bi bi-speedometer2", 1, null);
        MenuItemVO gestA   = crear("Gestión de Administradores", "/admin/administradores", "bi bi-people-fill", 2, null);
        MenuItemVO gestU   = crear("Gestión de Usuarios", "/admin/usuarios", "bi bi-person-lines-fill", 3, null);
        MenuItemVO config  = crear("Configuración", "/admin/configuracion", "bi bi-gear-fill", 4, null);
        // submenú ejemplo
        crear("Lista de Usuarios", "/admin/usuarios/lista", "bi bi-list-ul", 1, gestU);
        crear("Buscar Usuario", "/admin/usuarios/buscar", "bi bi-search", 2, gestU);

        // Menús Admin
        MenuItemVO gPI = crear("Gestión de Usuarios", "/admin/usuarios", "bi bi-people", 1, null);
        MenuItemVO eE  = crear("Envío de Emails", "/admin/emails", "bi bi-envelope", 2, null);
        MenuItemVO mP  = crear("Moderación de Propiedades", "/admin/propiedades", "bi bi-house", 3, null);
        MenuItemVO mV  = crear("Moderación de Valoraciones", "/admin/valoraciones", "bi bi-star", 4, null);
        crear("Propietarios", "/admin/usuarios/propietarios", "bi bi-person-badge", 1, gPI);
        crear("Inquilinos", "/admin/usuarios/inquilinos", "bi bi-person", 2, gPI);

        // Menús Propietario
        MenuItemVO mp = crear("Mis Propiedades", "/propietario/propiedades", "bi bi-house-door", 1, null);
        crear("Solicitudes de Reserva", "/propietario/reservas", "bi bi-calendar-check", 2, null);
        crear("Mensajes", "/mensajes/lista", "bi bi-chat", 3, null);
        crear("Valoraciones Recibidas", "/propietario/valoraciones", "bi bi-star-fill", 4, null);
        crear("Pagos Recibidos", "/propietario/pagos", "bi bi-cash-coin", 5, null);

        // Menús Inquilino
        MenuItemVO prop = crear("Propiedades", "/propiedades", "bi bi-houses", 1, null);
        crear("Mis Reservas", "/reservas/mis-reservas", "bi bi-calendar2-check", 2, null);
        crear("Mensajes", "/mensajes/lista", "bi bi-chat-dots", 3, null);
        crear("Mis Valoraciones", "/valoraciones/mis-valoraciones", "bi bi-star-half", 4, null);
        crear("Favoritos", "/inquilino/favoritos", "bi bi-heart-fill", 5, null);

        asignar(superAdmin, dash, gestA, gestU, config);
        asignar(admin, gPI, eE, mP, mV);
        asignar(propietario, mp);
        asignar(inquilino, prop);

        entityManager.flush();
        entityManager.clear();
    }

    private MenuItemVO crear(String nombre, String url, String icono, Integer orden, MenuItemVO padre) {
        MenuItemVO m = new MenuItemVO();
        m.setNombre(nombre);
        m.setUrl(url);
        m.setIcono(icono);
        m.setOrden(orden);
        m.setPadre(padre);
        m.setActivo(true);
        return menuItemRepository.save(m);
    }

    private void asignar(RolVO rol, MenuItemVO... items) {
        for (MenuItemVO m : items) {
            m.getRoles().add(rol);
            menuItemRepository.save(m);
        }
    }
}
