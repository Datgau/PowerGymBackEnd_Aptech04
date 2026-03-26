package com.example.project_backend04.util;

import com.example.project_backend04.exception.DatabaseRetryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.TransientDataAccessException;

import java.util.function.Supplier;

/**
 * Utility class for database operations with retry logic and exponential backoff
 */
@Slf4j
public class DatabaseRetryUtil {
    
    private static final int MAX_RETRIES = 3;
    private static final long BASE_DELAY_MS = 100; // 100ms

    public static <T> T executeWithRetry(Supplier<T> operation, String operationName) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.debug("Executing database operation '{}' - attempt {}/{}", operationName, attempt, MAX_RETRIES);
                T result = operation.get();
                
                if (attempt > 1) {
                    log.info("Database operation '{}' succeeded on attempt {}/{}", operationName, attempt, MAX_RETRIES);
                }
                
                return result;
                
            } catch (DataAccessException e) {
                lastException = e;
                log.warn("Database operation '{}' failed on attempt {}/{}: {}", 
                    operationName, attempt, MAX_RETRIES, e.getMessage());
                if (!isRetryableException(e) || attempt == MAX_RETRIES) {
                    break;
                }
                long delay = calculateDelay(attempt);
                try {
                    log.debug("Waiting {}ms before retry attempt {}", delay, attempt + 1);
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("Retry sleep interrupted for operation '{}'", operationName);
                    break;
                }
                
            } catch (Exception e) {
                log.error("Non-retryable exception in database operation '{}': {}", operationName, e.getMessage());
                throw e;
            }
        }
        
        log.error("Database operation '{}' failed after {} attempts", operationName, MAX_RETRIES);
        throw new DatabaseRetryException(operationName, MAX_RETRIES, MAX_RETRIES, lastException);
    }

    private static boolean isRetryableException(DataAccessException e) {
        if (e instanceof TransientDataAccessException) {
            return true;
        }
        String message = e.getMessage();
        if (message != null) {
            String lowerMessage = message.toLowerCase();
            return lowerMessage.contains("connection") ||
                   lowerMessage.contains("timeout") ||
                   lowerMessage.contains("deadlock") ||
                   lowerMessage.contains("lock wait timeout") ||
                   lowerMessage.contains("communications link failure");
        }
        
        return false;
    }

    private static long calculateDelay(int attempt) {
        long delay = BASE_DELAY_MS * (1L << (attempt - 1));
        
        double jitter = 0.25 * delay * (Math.random() * 2 - 1);
        delay += (long) jitter;
        return Math.min(delay, 5000);
    }
}