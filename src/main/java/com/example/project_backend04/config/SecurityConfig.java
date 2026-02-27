package com.example.project_backend04.config;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.security.CustomUserDetailsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthFilter;
    private final CustomUserDetailsService customUserDetailsService;

    @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    // Test commit

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    java.util.List<String> origins = java.util.Arrays.asList(allowedOrigins.split(","));
                    corsConfig.setAllowedOrigins(origins);
                    corsConfig.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                    corsConfig.setAllowedHeaders(java.util.Arrays.asList("*"));
                    corsConfig.setAllowCredentials(true);
                    corsConfig.setExposedHeaders(java.util.Arrays.asList("Set-Cookie", "Authorization"));
                    corsConfig.setMaxAge(3600L);
                    return corsConfig;
                }))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth
                            .requestMatchers("/api/auth/**").permitAll()
                            .requestMatchers("/ws/**").permitAll()
                            // ADMIN ONLY (Must be before public GET rules)
                            .requestMatchers("/api/admin/**").hasRole("ADMIN")
                            .requestMatchers("/api/stories/admin/**").hasRole("ADMIN")
                            // =====================
                            // PUBLIC GET ENDPOINTS
                            // =====================
                            .requestMatchers(HttpMethod.GET, "/api/gym/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/stories/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/stories").hasAnyRole("ADMIN", "USER", "STAFF")

                            .requestMatchers(HttpMethod.POST, "/api/gym/**").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers(HttpMethod.PUT, "/api/gym/**").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers(HttpMethod.DELETE, "/api/gym/**").hasAnyRole("ADMIN", "STAFF")
                            // AUTHENTICATED USERS
                            .anyRequest().authenticated();
                })
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

                            ApiResponse<?> apiResponse = new ApiResponse<>(
                                    false,
                                    "Full authentication is required to access this resource",
                                    null,
                                    HttpServletResponse.SC_UNAUTHORIZED
                            );
                            new ObjectMapper().writeValue(response.getWriter(), apiResponse);
                        })

                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                            ApiResponse<?> apiResponse = new ApiResponse<>(
                                    false,
                                    "You do not have permission to access this resource",
                                    null,
                                    HttpServletResponse.SC_FORBIDDEN
                            );

                            new ObjectMapper().writeValue(response.getWriter(), apiResponse);
                        })
                );

        return http.build();
    }

}



