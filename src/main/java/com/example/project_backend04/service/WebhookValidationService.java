package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.BankPayment.SepayWebhookRequest;
import com.example.project_backend04.exception.BankPaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Service for comprehensive webhook payload validation and sanitization
 * Requirements: 6.6
 */
@Service
@Slf4j
public class WebhookValidationService {

    // Patterns for input validation
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("^GYM_\\d+_\\d+$");
    private static final Pattern SAFE_STRING_PATTERN = Pattern.compile("^[\\p{L}0-9_\\-\\.\\s]*$");
    private static final Pattern TRANSACTION_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-]*$");
    
    // Security constraints
    private static final int MAX_DESCRIPTION_LENGTH = 500;
    private static final int MAX_TRANSACTION_ID_LENGTH = 100;
    private static final int MAX_AMOUNT_VALUE = 999999999;
    private static final int MIN_AMOUNT_VALUE = 1;

    /**
     * Validate and sanitize webhook payload structure
     * @param payload The webhook payload to validate
     * @throws BankPaymentException if payload is invalid or malformed
     */
    public void validateWebhookPayload(SepayWebhookRequest payload) {
        log.debug("Starting comprehensive webhook payload validation");
        
        try {
            // Validate required fields presence
            validateRequiredFields(payload);
            
            // Validate and sanitize description
            validateAndSanitizeDescription(payload);
            
            // Validate amount constraints
            validateAmount(payload);
            
            // Validate and sanitize transaction ID
            validateAndSanitizeTransactionId(payload);
            
            // Validate timestamp
            validateTimestamp(payload);
            
            log.debug("Webhook payload validation completed successfully");
            
        } catch (BankPaymentException e) {
            log.warn("Webhook payload validation failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during webhook payload validation", e);
            throw new BankPaymentException("Payload validation failed", "VALIDATION_ERROR", e);
        }
    }

    /**
     * Validate that all required fields are present and not null
     */
    private void validateRequiredFields(SepayWebhookRequest payload) {
        if (payload == null) {
            throw new BankPaymentException("Webhook payload is null", "INVALID_PAYLOAD");
        }
        
        if (!StringUtils.hasText(payload.getDescription())) {
            throw new BankPaymentException("Description is required and cannot be empty", "INVALID_DESCRIPTION");
        }
        
        if (payload.getAmount() == null) {
            throw new BankPaymentException("Amount is required", "INVALID_AMOUNT");
        }
    }

    /**
     * Validate and sanitize description field
     */
    private void validateAndSanitizeDescription(SepayWebhookRequest payload) {
        String description = payload.getDescription();
        
        // Length validation
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BankPaymentException(
                String.format("Description exceeds maximum length of %d characters", MAX_DESCRIPTION_LENGTH), 
                "DESCRIPTION_TOO_LONG");
        }
        
        // Sanitize: trim whitespace
        description = description.trim();
        payload.setDescription(description);
        
        // Security validation: check for potentially malicious content first
        checkForInjectionAttempts(description, "description");
        
        // Then check for general invalid characters
        if (!SAFE_STRING_PATTERN.matcher(description).matches()) {
            throw new BankPaymentException("Description contains invalid or potentially unsafe characters", 
                "INVALID_DESCRIPTION_FORMAT");
        }
        
        // Business logic validation: check expected format for PowerGym payments
        if (!DESCRIPTION_PATTERN.matcher(description).matches()) {
            log.warn("Description does not match expected PowerGym format: {}", description);
            // Note: We don't throw exception here as external payments might have different formats
            // But we log it for monitoring purposes
        }
    }

    /**
     * Validate amount constraints
     */
    private void validateAmount(SepayWebhookRequest payload) {
        Integer amount = payload.getAmount();
        
        if (amount < MIN_AMOUNT_VALUE) {
            throw new BankPaymentException(
                String.format("Amount must be at least %d", MIN_AMOUNT_VALUE), 
                "AMOUNT_TOO_SMALL");
        }
        
        if (amount > MAX_AMOUNT_VALUE) {
            throw new BankPaymentException(
                String.format("Amount exceeds maximum allowed value of %d", MAX_AMOUNT_VALUE), 
                "AMOUNT_TOO_LARGE");
        }
    }

    /**
     * Validate and sanitize transaction ID
     */
    private void validateAndSanitizeTransactionId(SepayWebhookRequest payload) {
        String transactionId = payload.getTransactionId();
        
        if (transactionId != null) {
            // Length validation
            if (transactionId.length() > MAX_TRANSACTION_ID_LENGTH) {
                throw new BankPaymentException(
                    String.format("Transaction ID exceeds maximum length of %d characters", MAX_TRANSACTION_ID_LENGTH), 
                    "TRANSACTION_ID_TOO_LONG");
            }
            
            // Sanitize: trim whitespace
            transactionId = transactionId.trim();
            
            if (!transactionId.isEmpty()) {
                // Security validation: check for injection attempts first
                checkForInjectionAttempts(transactionId, "transactionId");
                
                // Then check for general invalid characters
                if (!TRANSACTION_ID_PATTERN.matcher(transactionId).matches()) {
                    throw new BankPaymentException("Transaction ID contains invalid characters", 
                        "INVALID_TRANSACTION_ID_FORMAT");
                }
            }
            
            payload.setTransactionId(transactionId.isEmpty() ? null : transactionId);
        }
    }

    /**
     * Validate timestamp constraints
     */
    private void validateTimestamp(SepayWebhookRequest payload) {
        LocalDateTime timestamp = payload.getTimestamp();
        
        if (timestamp != null) {
            LocalDateTime now = LocalDateTime.now();
            
            // Prevent future timestamps (potential security issue)
            if (timestamp.isAfter(now)) {
                throw new BankPaymentException("Timestamp cannot be in the future", "INVALID_TIMESTAMP");
            }
            
            // Prevent very old timestamps (potential replay attack)
            LocalDateTime oneWeekAgo = now.minusWeeks(1);
            if (timestamp.isBefore(oneWeekAgo)) {
                log.warn("Received webhook with very old timestamp: {}", timestamp);
                // Note: We log but don't reject as legitimate delayed webhooks might occur
            }
        }
    }

    /**
     * Perform additional security checks on the payload
     */
    private void performSecurityChecks(SepayWebhookRequest payload) {
        // Check for potential injection attempts in string fields
        checkForInjectionAttempts(payload.getDescription(), "description");
        
        if (payload.getTransactionId() != null) {
            checkForInjectionAttempts(payload.getTransactionId(), "transactionId");
        }
        
        // Log security-relevant information for monitoring
        log.debug("Security validation passed for webhook payload - Description: {}, Amount: {}", 
            payload.getDescription(), payload.getAmount());
    }

    /**
     * Check for potential injection attempts in string fields
     */
    private void checkForInjectionAttempts(String value, String fieldName) {
        if (value == null) return;
        
        String lowerValue = value.toLowerCase();
        
        // Check for common injection patterns
        String[] suspiciousPatterns = {
            "script", "javascript", "vbscript", "onload", "onerror",
            "select", "union", "insert", "update", "delete", "drop",
            "../", "..\\", "<", ">", "eval(", "exec("
        };
        
        for (String pattern : suspiciousPatterns) {
            if (lowerValue.contains(pattern)) {
                log.warn("Potential injection attempt detected in {}: {}", fieldName, value);
                throw new BankPaymentException(
                    String.format("Field %s contains potentially malicious content", fieldName), 
                    "SECURITY_VIOLATION");
            }
        }
    }
}