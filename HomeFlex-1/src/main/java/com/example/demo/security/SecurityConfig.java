package com.example.demo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    
    @Autowired
    private CustomAuthenticationFailureHandler authenticationFailureHandler;
    
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas
                .requestMatchers("/", "/login", "/registro", "/activar", "/css/**", "/js/**", "/images/**").permitAll()
                
                // Rutas para super administrador
                .requestMatchers("/admin/system/**").hasRole("SUPER_ADMIN")
                
                // Rutas para administradores y superAdmin
                .requestMatchers("/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                
                // Rutas para propietarios
                .requestMatchers("/propietario/**").hasAnyRole("PROPIETARIO", "ADMIN", "SUPER_ADMIN")
                
                // Rutas para inquilinos
                .requestMatchers("/inquilino/**").hasAnyRole("INQUILINO", "ADMIN", "SUPER_ADMIN")
                
                // Dashboard general - accesible para usuarios autenticados
                .requestMatchers("/dashboard/**").authenticated()
                
                // Cualquier otra ruta requiere autenticación
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .failureHandler(authenticationFailureHandler)
                .defaultSuccessUrl("/dashboard")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        
        return http.build();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 3); 
    }
}