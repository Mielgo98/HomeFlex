package com.example.demo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.demo.jwt.JwtAuthenticationEntryPoint;
import com.example.demo.jwt.JwtAuthenticationFilter;

import java.util.Collections;

/**
 * Configuración de seguridad para la aplicación
 * Combina autenticación basada en formularios para la web y JWT para la API
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Habilita anotaciones de seguridad como @PreAuthorize
public class SecurityConfig {
    
    @Autowired
    private CustomAuthenticationFailureHandler authenticationFailureHandler;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    /**
     * Configura el proveedor de autenticación
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    /**
     * Configura el gestor de autenticación
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(Collections.singletonList(authenticationProvider()));
    }
    
    /**
     * Configura la cadena de filtros de seguridad
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Deshabilitar CSRF para APIs RESTful
            .csrf(AbstractHttpConfigurer::disable)
            
            // Configurar reglas de autorización de peticiones
            .authorizeHttpRequests(authorize -> authorize
                // Rutas públicas para la web
                .requestMatchers("/", "/login", "/registro", "/activar", "/css/**", "/js/**", "/images/**").permitAll()
                // Rutas públicas para la API
                .requestMatchers("/api/auth/**", "/api/test/all").permitAll()
                // Rutas protegidas para la API
                .requestMatchers("/api/**").authenticated()
                // Rutas web para usuarios autenticados
                .requestMatchers("/dashboard/**").authenticated()
                // Cualquier otra petición requiere autenticación
                .anyRequest().authenticated()
            )
            
            // Configuración de login basado en formularios (para la web)
            .formLogin(form -> form
                .loginPage("/login")
                .failureHandler(authenticationFailureHandler)
                .defaultSuccessUrl("/dashboard")
                .permitAll()
            )
            
            // Configuración de logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            )
            
            // Configuración para API REST con JWT
            .sessionManagement(session -> session
                // Sin estado para APIs (no mantener sesión)
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            
            // Configurar manejo de excepciones
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )
            
            // Configurar proveedor de autenticación
            .authenticationProvider(authenticationProvider())
            
            // Añadir filtro JWT antes del filtro de autenticación estándar
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
    
    /**
     * Configurar codificador de contraseñas
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}