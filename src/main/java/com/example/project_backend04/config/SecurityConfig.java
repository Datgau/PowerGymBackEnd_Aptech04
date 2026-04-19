package com.example.project_backend04.config;

import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.example.project_backend04.security.CustomUserDetailsService;
import com.example.project_backend04.security.WebhookAuthenticationFilter;
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
    private final WebhookAuthenticationFilter webhookAuthenticationFilter;
    private final ObjectMapper objectMapper;

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
                            // AI CHATBOT - PUBLIC ACCESS
                            .requestMatchers("/api/chat/**").permitAll()
                            // ADMIN & STAFF ONLY (Must be before public GET rules)
                            .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers("/api/stories/admin/**").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers("/api/admin/rewards/**").hasAnyRole("ADMIN", "STAFF")
                            // PUBLIC GET ENDPOINTS
                            .requestMatchers(HttpMethod.GET, "/api/gym/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/stories/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/membership-packages/active").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/user/memberships/**").authenticated()
                            .requestMatchers(HttpMethod.GET, "/api/equipments/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/equipment-categories/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/bookings/trainers/**").permitAll()

                            // STATISTICS - ADMIN & STAFF ONLY (Must be before /api/products/**)
                            .requestMatchers("/api/statistics/**").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers(HttpMethod.GET, "/api/products/statistics").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers(HttpMethod.GET, "/api/product-orders/statistics").hasAnyRole("ADMIN", "STAFF")

                            // PRODUCTS - PUBLIC GET, ADMIN & STAFF WRITE
                            .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/products/**").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers(HttpMethod.PUT, "/api/products/**").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasAnyRole("ADMIN", "STAFF")

                            // PRODUCT ORDERS - AUTHENTICATED USERS
                            .requestMatchers(HttpMethod.GET, "/api/product-orders/**").authenticated()
                            .requestMatchers(HttpMethod.POST, "/api/product-orders").authenticated()
                            .requestMatchers(HttpMethod.PUT, "/api/product-orders/*/payment-status").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers(HttpMethod.PUT, "/api/product-orders/*/delivery-status").hasAnyRole("ADMIN", "STAFF")

                            // IMPORT RECEIPTS - ADMIN & STAFF ONLY
                            .requestMatchers("/api/import-receipts/**").hasAnyRole("ADMIN", "STAFF")

                            // MEMBERSHIP PACKAGES - ADMIN & STAFF FOR WRITE OPERATIONS
                            .requestMatchers(HttpMethod.GET, "/api/membership-packages/**").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers(HttpMethod.POST, "/api/membership-packages/**").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers(HttpMethod.PUT, "/api/membership-packages/**").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers(HttpMethod.DELETE, "/api/membership-packages/**").hasAnyRole("ADMIN", "STAFF")

                            // EQUIPMENTS - ADMIN & STAFF FOR WRITE OPERATIONS
                            .requestMatchers(HttpMethod.POST, "/api/equipments/**").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers(HttpMethod.PUT, "/api/equipments/**").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers(HttpMethod.DELETE, "/api/equipments/**").hasAnyRole("ADMIN", "STAFF")

                            // EQUIPMENT CATEGORIES - ADMIN & STAFF FOR WRITE OPERATIONS
                            .requestMatchers(HttpMethod.POST, "/api/equipment-categories/**").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers(HttpMethod.PUT, "/api/equipment-categories/**").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers(HttpMethod.DELETE, "/api/equipment-categories/**").hasAnyRole("ADMIN", "STAFF")
                            
                            // PROMOTIONS - FEATURED ONLY PUBLIC, REST REQUIRES AUTH
                            .requestMatchers(HttpMethod.GET, "/api/promotions/featured").permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/promotions/active").authenticated()
                            .requestMatchers(HttpMethod.POST, "/api/promotions/apply").authenticated()
                            .requestMatchers("/api/promotions/**").hasAnyRole("ADMIN", "STAFF") // Admin & Staff management
                            
                            // REWARDS - AUTHENTICATED ONLY (USER-SPECIFIC DATA)
                            .requestMatchers("/api/rewards/**").authenticated()
                            
                            // AUTHENTICATED ENDPOINTS
                            .requestMatchers("/api/payment/**").authenticated()
                            .requestMatchers(HttpMethod.POST, "/api/payment/momo/ipn").permitAll() // MoMo IPN callback doesn't need auth
                            
                            // BANK PAYMENT ENDPOINTS
                            .requestMatchers("/api/bank-payments/create").authenticated()
                            .requestMatchers("/api/bank-payments/status/**").authenticated()
                            .requestMatchers(HttpMethod.POST, "/api/bank-payments/webhook").permitAll() // SePay webhook - secured by WebhookAuthenticationFilter

                            // INVOICE ENDPOINTS
                            .requestMatchers("/api/invoices/**").authenticated()

                            // TRAINER SALARY ENDPOINTS - Method-level security via @PreAuthorize
                            .requestMatchers("/api/trainers/*/salary").authenticated()

                            .requestMatchers(HttpMethod.POST, "/api/stories").hasAnyRole("ADMIN", "USER", "STAFF")
                            .requestMatchers(HttpMethod.POST, "/api/gym/**").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers(HttpMethod.PUT, "/api/gym/**").hasAnyRole("ADMIN", "STAFF")
                            .requestMatchers(HttpMethod.DELETE, "/api/gym/**").hasAnyRole("ADMIN", "STAFF")
                            .anyRequest().authenticated();
                })
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(webhookAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
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
                            objectMapper.writeValue(response.getWriter(), apiResponse);
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

                            objectMapper.writeValue(response.getWriter(), apiResponse);
                        })
                );

        return http.build();
    }

}



