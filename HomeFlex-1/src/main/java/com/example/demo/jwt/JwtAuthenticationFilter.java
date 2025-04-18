package com.example.demo.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserDetailsService userDetailsService;

    @Value("${jwt.header}")
    private String tokenHeader;

    @Value("${jwt.prefix}")
    private String tokenPrefix;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        // Obtener el header de autorización
        final String header = request.getHeader(tokenHeader);

        // Verificar si hay un token válido
        if (header == null || !header.startsWith(tokenPrefix)) {
            chain.doFilter(request, response);
            return;
        }

        // Extraer el token eliminando el prefijo
        final String token = header.substring(tokenPrefix.length());

        try {
            // Extraer el username del token
            String username = jwtUtils.getUsernameFromToken(token);

            // Verificar si el username es válido y no hay autenticación ya establecida
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Cargar los detalles del usuario
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // Validar el token
                if (jwtUtils.validateToken(token, userDetails)) {
                    
                    // Crear una autenticación válida
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // Establecer la autenticación en el contexto
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    
                    logger.info("Usuario autenticado: " + username);
                }
            }
        } catch (Exception e) {
            logger.error("Error procesando el token JWT: " + e.getMessage());
        }

        chain.doFilter(request, response);
    }
}