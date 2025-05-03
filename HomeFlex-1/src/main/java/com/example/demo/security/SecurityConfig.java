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
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

import com.example.demo.jwt.JwtAuthenticationEntryPoint;
import com.example.demo.jwt.JwtAuthenticationFilter;

import java.util.Collections;

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

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    
    @Autowired
    private AuditLogFilter auditLogFilter;

    /**
     * Ignora rutas estáticas para que no pasen por los filtros de seguridad.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
            .requestMatchers(
                new AntPathRequestMatcher("/css/**"),
                new AntPathRequestMatcher("/js/**"),
                new AntPathRequestMatcher("/images/**"),
                new AntPathRequestMatcher("/webjars/**"),
                new AntPathRequestMatcher("/favicon.ico")
            );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Argon2 es más seguro que BCrypt
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Necesario para inyectar en JwtAuthenticationFilter (si lo usas manualmente).
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        return new ProviderManager(Collections.singletonList(authenticationProvider()));
    }

    /**
     * Cadena de seguridad para la API REST (/api/**).
     * Stateless, sin form-login, devuelve 401 vía JwtAuthenticationEntryPoint.
     */
    @Configuration
    @Order(1)
    public class ApiSecurityConfig {
        @Bean
        public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
            http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/loginPEE").permitAll()
                    .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

            return http.build();
        }
    }

    /**
     * Cadena de seguridad para el frontend Thymeleaf (form-login clásico).
     */
    @Configuration
    @Order(2)
    public class WebSecurityConfig {
    	   @Bean
    	    public SecurityFilterChain webSecurityFilterChain(HttpSecurity http) throws Exception {
    	        http
    	            .csrf(AbstractHttpConfigurer::disable)
    	            .authorizeHttpRequests(auth -> auth
    	                .requestMatchers(
    	                    "/", "/index", "/login", "/login?error", "/login?logout",
    	                    "/registro", "/activar", "/cuenta-eliminada"
    	                ).permitAll()
    	                .anyRequest().authenticated()
    	            )
    	            .formLogin(form -> form
    	                .loginPage("/login")
    	                .loginProcessingUrl("/login")
    	                .failureHandler(authenticationFailureHandler)
    	                .successHandler(authenticationSuccessHandler)
    	                .permitAll()
    	            )
    	            .logout(logout -> logout
    	                .logoutUrl("/logout")
    	                .logoutSuccessUrl("/login?logout=true")
    	                .invalidateHttpSession(true)
    	                .deleteCookies("JSESSIONID", "jwt_token")
    	                .permitAll()
    	            )
    	            .sessionManagement(sm -> sm
    	                    .sessionFixation(sessionFixation -> sessionFixation.none())
    	                )
    	            .authenticationProvider(authenticationProvider())
    	            // ← Aquí añadimos el AuditLogFilter
    	            .addFilterBefore(auditLogFilter, UsernamePasswordAuthenticationFilter.class);
    	        return http.build();
    	    }
    }
}
