package com.example.demo.security;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.demo.jwt.JwtUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        
        // Generar el token JWT
        String jwtToken = jwtUtils.generateJwtToken(authentication);
        
        // Guardar el token en una cookie
        Cookie jwtCookie = new Cookie("jwt_token", jwtToken);
        jwtCookie.setPath("/");
        jwtCookie.setHttpOnly(true); //No accesible desde js
        jwtCookie.setMaxAge(60 * 60 * 24); // 24 horas en segundos
        
        jwtCookie.setSecure(true); // Solo para HTTPS
        
        response.addCookie(jwtCookie);
        
        // Guardar el token en la sesión también (opcional, dependiendo de tu estrategia)
        request.getSession().setAttribute("jwt_token", jwtToken);
        
        // Continúa con el comportamiento normal (redirección a la página de éxito)
        super.onAuthenticationSuccess(request, response, authentication);
    }
}