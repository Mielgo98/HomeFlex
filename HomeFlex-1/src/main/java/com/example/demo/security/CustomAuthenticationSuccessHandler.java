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

    public CustomAuthenticationSuccessHandler() {
        super();
        setDefaultTargetUrl("/index");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
                                        throws IOException, ServletException {
        // Generar el JWT
        String jwt = jwtUtils.generateJwtToken(authentication);

        // Crear y añadir la cookie accesible desde JS
        Cookie cookie = new Cookie("jwt_token", jwt);
        cookie.setPath("/");
        cookie.setMaxAge(60 * 60 * 24); // 1 día
        cookie.setHttpOnly(false);
        response.addCookie(cookie);

        response.sendRedirect("/index");
    }
}
