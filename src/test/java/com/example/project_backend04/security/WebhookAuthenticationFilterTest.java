package com.example.project_backend04.security;

import com.example.project_backend04.config.BankPaymentConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class WebhookAuthenticationFilterTest {

    @Mock
    private BankPaymentConfig bankPaymentConfig;

    @Mock
    private BankPaymentConfig.WebhookConfig webhookConfig;
    
    @Mock
    private HttpServletRequest request;
    
    @Mock
    private HttpServletResponse response;
    
    @Mock
    private FilterChain filterChain;
    
    private WebhookAuthenticationFilter filter;
    private StringWriter responseWriter;
    
    @BeforeEach
    void setUp() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        filter = new WebhookAuthenticationFilter(bankPaymentConfig, mapper);
        lenient().when(bankPaymentConfig.webhook()).thenReturn(webhookConfig);
        lenient().when(webhookConfig.rateLimitPerMinute()).thenReturn(100);
        lenient().when(webhookConfig.apiKeyHeader()).thenReturn("X-API-Key");
        lenient().when(webhookConfig.path()).thenReturn("/api/bank-payments/webhook");
        
        responseWriter = new StringWriter();
        lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }
    
    @Test
    void shouldAllowNonWebhookRequests() throws ServletException, IOException {
        // Given
        when(request.getServletPath()).thenReturn("/api/other/endpoint");
        when(request.getMethod()).thenReturn("POST");
        
        // When
        filter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }
    
    @Test
    void shouldAllowGetRequestsToWebhookPath() throws ServletException, IOException {
        // Given
        when(request.getServletPath()).thenReturn("/api/bank-payments/webhook");
        when(request.getMethod()).thenReturn("GET");
        
        // When
        filter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }
    
    @Test
    void shouldRejectWebhookRequestWithoutApiKey() throws ServletException, IOException {
        // Given
        when(request.getServletPath()).thenReturn("/api/bank-payments/webhook");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getContentType()).thenReturn("application/json");
        
        // When
        filter.doFilter(request, response, filterChain);
        
        // Then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(filterChain, never()).doFilter(request, response);
    }
    
    @Test
    void shouldRejectWebhookRequestWithInvalidApiKey() throws ServletException, IOException {
        // Given
        when(request.getServletPath()).thenReturn("/api/bank-payments/webhook");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-API-Key")).thenReturn("invalid-key");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getContentType()).thenReturn("application/json");
        when(bankPaymentConfig.sepayApiKey()).thenReturn("valid-api-key");
        
        // When
        filter.doFilter(request, response, filterChain);
        
        // Then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(response).setContentType("application/json;charset=UTF-8");
        verify(filterChain, never()).doFilter(request, response);
    }
    
    @Test
    void shouldAllowWebhookRequestWithValidApiKey() throws ServletException, IOException {
        // Given
        when(request.getServletPath()).thenReturn("/api/bank-payments/webhook");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-API-Key")).thenReturn("valid-api-key");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getContentType()).thenReturn("application/json");
        when(bankPaymentConfig.sepayApiKey()).thenReturn("valid-api-key");
        
        // When
        filter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }
    
    @Test
    void shouldEnforceRateLimiting() throws ServletException, IOException {
        // Given
        when(request.getServletPath()).thenReturn("/api/bank-payments/webhook");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-API-Key")).thenReturn("valid-api-key");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getContentType()).thenReturn("application/json");
        when(bankPaymentConfig.sepayApiKey()).thenReturn("valid-api-key");
        
        // When - Make 101 requests (exceeding the 100 per minute limit)
        for (int i = 0; i < 101; i++) {
            filter.doFilter(request, response, filterChain);
        }
        
        // Then - The 101st request should be rate limited
        verify(response, atLeastOnce()).setStatus(429); // 429 Too Many Requests
    }
    
    @Test
    void shouldExtractClientIPFromXForwardedForHeader() throws ServletException, IOException {
        // Given
        when(request.getServletPath()).thenReturn("/api/bank-payments/webhook");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-API-Key")).thenReturn("valid-api-key");
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1");
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(request.getContentType()).thenReturn("application/json");
        when(bankPaymentConfig.sepayApiKey()).thenReturn("valid-api-key");
        
        // When
        filter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        // The filter should use the first IP from X-Forwarded-For (192.168.1.1)
    }
    
    @Test
    void shouldExtractClientIPFromXRealIPHeader() throws ServletException, IOException {
        // Given
        when(request.getServletPath()).thenReturn("/api/bank-payments/webhook");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-API-Key")).thenReturn("valid-api-key");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("192.168.1.1");
        lenient().when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(request.getContentType()).thenReturn("application/json");
        when(bankPaymentConfig.sepayApiKey()).thenReturn("valid-api-key");
        
        // When
        filter.doFilter(request, response, filterChain);
        
        // Then
        verify(filterChain).doFilter(request, response);
        // The filter should use X-Real-IP (192.168.1.1)
    }
    
    @Test
    void shouldHandleEmptyApiKeyConfiguration() throws ServletException, IOException {
        // Given
        when(request.getServletPath()).thenReturn("/api/bank-payments/webhook");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-API-Key")).thenReturn("any-key");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getContentType()).thenReturn("application/json");
        when(bankPaymentConfig.sepayApiKey()).thenReturn(""); // Empty configuration
        
        // When
        filter.doFilter(request, response, filterChain);
        
        // Then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }
    
    @Test
    void shouldHandleNullApiKeyConfiguration() throws ServletException, IOException {
        // Given
        when(request.getServletPath()).thenReturn("/api/bank-payments/webhook");
        when(request.getMethod()).thenReturn("POST");
        when(request.getHeader("X-API-Key")).thenReturn("any-key");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(request.getHeader("User-Agent")).thenReturn("Test-Agent");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getContentType()).thenReturn("application/json");
        when(bankPaymentConfig.sepayApiKey()).thenReturn(null); // Null configuration
        
        // When
        filter.doFilter(request, response, filterChain);
        
        // Then
        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        verify(filterChain, never()).doFilter(request, response);
    }
}