package com.example.demo.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
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

        String token = null;
        
        // 1. Primero intenta obtener el token del encabezado Authorization
        final String header = request.getHeader(tokenHeader);
        if (header != null && header.startsWith(tokenPrefix)) {
            token = header.substring(tokenPrefix.length());
        }
        
        // 2. Si no hay token en el encabezado, intenta obtenerlo de las cookies
        if (token == null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if ("jwt_token".equals(cookie.getName())) {
                        token = cookie.getValue();
                        break;
                    }
                }
            }
        }
        
        // 3. Si aún no hay token, intenta obtenerlo de la sesión (opcional)
        if (token == null && request.getSession() != null) {
            Object sessionToken = request.getSession().getAttribute("jwt_token");
            if (sessionToken != null) {
                token = sessionToken.toString();
            }
        }

        try {
            // Verificar si hay un token válido
            if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // Extraer el username del token
                String username = jwtUtils.getUsernameFromToken(token);

                // Verificar si el username es válido
                if (username != null) {
                    
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
            }
        } catch (Exception e) {
            logger.error("Error procesando el token JWT: " + e.getMessage());
        }

        chain.doFilter(request, response);
    }
}