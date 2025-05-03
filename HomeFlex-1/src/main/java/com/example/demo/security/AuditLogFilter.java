package com.example.demo.security;

import java.io.IOException;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.example.demo.admin.model.LogAccesoVO;
import com.example.demo.admin.service.LogAccesoService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
@Component
public class AuditLogFilter extends OncePerRequestFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditLogFilter.class);

    @Autowired
    private LogAccesoService logAccesoService;
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        // <-- Si es la ruta de login, no registramos
        if (uri.equals("/login") || uri.equals("/login-page")) {
            filterChain.doFilter(request, response);
            return;
        }

        // 1) Construimos el VO
        LogAccesoVO log = new LogAccesoVO();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.setUsuario(auth != null ? auth.getName() : "ANÓNIMO");
        log.setAccion(request.getMethod());
        log.setFecha(LocalDateTime.now());
        log.setUrl(uri);

        // 2) Guardamos sin bloquear la petición si falla
        try {
            logAccesoService.registrar(log);
        } catch (Exception ex) {
            LOGGER.warn("No se pudo guardar log de acceso ({} {}): {}",
                request.getMethod(), uri, ex.getMessage());
        }

        // 3) Continúa la cadena
        filterChain.doFilter(request, response);
    }
}
