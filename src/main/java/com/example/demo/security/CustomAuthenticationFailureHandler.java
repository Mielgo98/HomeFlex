package com.example.demo.security;

import java.io.IOException;

import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        
        String redirectUrl = "/login?error";
        
        // Detectar si el error es debido a una cuenta deshabilitada (no activada)
        if (exception instanceof DisabledException) {
            redirectUrl = "/login?disabled";
            System.out.println("Login fallido: cuenta no activada");
        } else {
            System.out.println("Login fallido: " + exception.getMessage());
        }
        
        // Redirigir a la URL correspondiente
        super.setDefaultFailureUrl(redirectUrl);
        super.onAuthenticationFailure(request, response, exception);
    }
}