package com.example.demo.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.example.demo.jwt.JwtAuthenticationEntryPoint;
import com.example.demo.jwt.JwtAuthenticationFilter;

import java.util.Collections;

/**
 * Configuración de seguridad para la aplicación
 * Separamos la configuración para la interfaz web y la API REST
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    
    @Autowired
    private CustomAuthenticationFailureHandler authenticationFailureHandler;
    
    @Autowired
    private CustomAuthenticationSuccessHandler authenticationSuccessHandler;
    
    @Autowired
    private UserDetailsService userDetailsService;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }
    
    /**
     * Configura el proveedor de autenticación común
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    /**
     * Configura el gestor de autenticación común
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(Collections.singletonList(authenticationProvider()));
    }
    
    /**
     * Configuración para la interfaz web (basada en formularios y sesiones)
     */
    @Configuration
    @Order(2) // Primera cadena de filtros a evaluar
    public static class WebSecurityConfig {
        
        @Autowired
        private CustomAuthenticationFailureHandler authenticationFailureHandler;
        
        @Autowired
        private CustomAuthenticationSuccessHandler authenticationSuccessHandler;
        
        @Autowired
        private DaoAuthenticationProvider authenticationProvider;
        
        @Bean
        public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .securityMatcher("/**") // Aplicar a todas las rutas excepto /api/**
                .authorizeHttpRequests(authorize -> authorize
                    // Rutas públicas
                    .requestMatchers("/", "/login", "/registro", "/activar", "/cuenta-eliminada", "/css/**", "/js/**", "/images/**").permitAll()
                    // Rutas protegidas
                    .requestMatchers("/dashboard/**", "/perfil/**").authenticated()
                    .anyRequest().authenticated()
                )
                .formLogin(form -> form
                    .loginPage("/login")
                    .failureHandler(authenticationFailureHandler)
                    .successHandler(authenticationSuccessHandler) // Aquí agregamos nuestro success handler
                    .defaultSuccessUrl("/dashboard")
                    .permitAll()
                )
                .logout(logout -> logout
                    .logoutUrl("/logout")
                    .logoutSuccessUrl("/login?logout")
                    .invalidateHttpSession(true)
                    .deleteCookies("JSESSIONID", "jwt_token") // Agregar jwt_token a las cookies eliminadas durante logout
                    .permitAll()
                )
                .authenticationProvider(authenticationProvider);
            
            return http.build();
        }
    }
    
    /**
     * Configuración para la API REST (basada en JWT)
     */
    @Configuration
    @Order(1) // Segunda cadena de filtros a evaluar
    public static class ApiSecurityConfig {
        
        @Autowired
        private JwtAuthenticationFilter jwtAuthenticationFilter;
        
        @Autowired
        private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
        
        @Autowired
        private DaoAuthenticationProvider authenticationProvider;
        
        @Bean
        public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .securityMatcher("/api/**") // Aplicar solo a /api/**
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(authorize -> authorize
                    // Rutas públicas de la API
                    .requestMatchers("/api/auth/**").permitAll()
                    // Cualquier otra ruta de la API requiere autenticación
                    .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                    .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
            
            return http.build();
        }
    }
}