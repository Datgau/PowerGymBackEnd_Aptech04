package com.example.project_backend04.security;

import com.example.project_backend04.config.BankPaymentConfig;
import com.example.project_backend04.dto.response.Shared.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookAuthenticationFilter implements Filter {

    private final BankPaymentConfig bankPaymentConfig;
    private final ObjectMapper objectMapper;

    private final ConcurrentHashMap<String, AtomicInteger> requestCounts = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastResetTime = new ConcurrentHashMap<>();


    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String requestPath = httpRequest.getServletPath();
        String method = httpRequest.getMethod();
        String clientIP = getClientIP(httpRequest);

        if (!isWebhookEndpoint(requestPath, method)) {
            chain.doFilter(request, response);
            return;
        }

        logWebhookAttempt(httpRequest, clientIP);

        if (!checkRateLimit(clientIP)) {
            logSecurityEvent("RATE_LIMIT_EXCEEDED", clientIP, requestPath, "Rate limit exceeded");
            sendErrorResponse(httpResponse, 429, // 429 Too Many Requests
                "Rate limit exceeded. Maximum " + bankPaymentConfig.webhook().rateLimitPerMinute() + " requests per minute allowed.");
            return;
        }

        String apiKey = httpRequest.getHeader(bankPaymentConfig.webhook().apiKeyHeader());
        

        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = httpRequest.getHeader("Authorization");
        }
        
        if (!isValidApiKey(apiKey)) {
            logSecurityEvent("INVALID_API_KEY", clientIP, requestPath, 
                apiKey == null ? "Missing API key" : "Invalid API key");
            sendErrorResponse(httpResponse, HttpServletResponse.SC_UNAUTHORIZED, 
                "Invalid or missing API key");
            return;
        }
        logSecurityEvent("WEBHOOK_AUTH_SUCCESS", clientIP, requestPath, "Authentication successful");
        chain.doFilter(request, response);
    }

    private boolean isWebhookEndpoint(String path, String method) {
        return "POST".equals(method) && bankPaymentConfig.webhook().path().equals(path);
    }

    private boolean isValidApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        
        String configuredApiKey = bankPaymentConfig.sepayApiKey();
        if (configuredApiKey == null || configuredApiKey.trim().isEmpty()) {
            log.warn("SePay API key not configured in application properties");
            return false;
        }
        
        String extractedKey = apiKey.trim();
        if (extractedKey.toLowerCase().startsWith("apikey ")) {
            extractedKey = extractedKey.substring(7).trim();
        } else if (extractedKey.toLowerCase().startsWith("bearer ")) {
            extractedKey = extractedKey.substring(7).trim();
        }
        
        return configuredApiKey.equals(extractedKey);
    }

    private boolean checkRateLimit(String clientIP) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime currentMinute = now.withSecond(0).withNano(0);
        
        // Get or create request count for this IP
        AtomicInteger count = requestCounts.computeIfAbsent(clientIP, k -> new AtomicInteger(0));
        LocalDateTime lastReset = lastResetTime.get(clientIP);
        
        // Reset counter if we're in a new minute
        if (lastReset == null || lastReset.isBefore(currentMinute)) {
            count.set(0);
            lastResetTime.put(clientIP, currentMinute);
        }
        
        // Increment and check limit
        int currentCount = count.incrementAndGet();
        
        // Clean up old entries (optional optimization)
        cleanupOldEntries(currentMinute);
        
        return currentCount <= bankPaymentConfig.webhook().rateLimitPerMinute();
    }
    
    /**
     * Clean up rate limiting entries older than 2 minutes to prevent memory leaks
     */
    private void cleanupOldEntries(LocalDateTime currentMinute) {
        LocalDateTime cutoff = currentMinute.minusMinutes(2);
        
        lastResetTime.entrySet().removeIf(entry -> entry.getValue().isBefore(cutoff));
        
        // Remove corresponding request counts
        requestCounts.entrySet().removeIf(entry -> 
            !lastResetTime.containsKey(entry.getKey()));
    }
    
    /**
     * Extract client IP address from request, considering proxy headers
     */
    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Take the first IP in case of multiple proxies
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIP = request.getHeader("X-Real-IP");
        if (xRealIP != null && !xRealIP.isEmpty()) {
            return xRealIP;
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * Log webhook attempt with basic request information for audit purposes
     */
    private void logWebhookAttempt(HttpServletRequest request, String clientIP) {
        String userAgent = request.getHeader("User-Agent");
        String contentType = request.getContentType();
        
        // Structured audit log for webhook attempts
        log.info("AUDIT_LOG - WEBHOOK_ATTEMPT - IP: {}, Path: {}, Method: {}, User-Agent: {}, Content-Type: {}, Timestamp: {}", 
            clientIP, request.getServletPath(), request.getMethod(), userAgent, contentType, 
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME));
    }
    
    /**
     * Log security events with structured format for audit and monitoring
     */
    private void logSecurityEvent(String eventType, String clientIP, String path, String details) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        // Enhanced structured audit log for security events
        log.warn("AUDIT_LOG - SECURITY_EVENT - Type: {}, IP: {}, Path: {}, Details: {}, Timestamp: {}", 
            eventType, clientIP, path, details, timestamp);
    }
    
    /**
     * Send standardized error response
     */
    private void sendErrorResponse(HttpServletResponse response, int statusCode, String message) 
            throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(statusCode);
        
        ApiResponse<?> apiResponse = new ApiResponse<>(
            false,
            message,
            null,
            statusCode
        );
        
        objectMapper.writeValue(response.getWriter(), apiResponse);
    }
    
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("WebhookAuthenticationFilter initialized - Rate limit: {} requests/minute",
                bankPaymentConfig.webhook().rateLimitPerMinute());
    }
    
    @Override
    public void destroy() {
        requestCounts.clear();
        lastResetTime.clear();
        log.info("WebhookAuthenticationFilter destroyed");
    }
}