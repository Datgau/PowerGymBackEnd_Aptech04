package com.example.project_backend04.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpService {

    private static final String OTP_CACHE_NAME = "otpCache";
    private static final int OTP_LENGTH = 6;
    private final SecureRandom secureRandom = new SecureRandom();
    private final CacheManager cacheManager;

    /**
     * Generate a random 6-digit OTP
     */
    public String generateOtp() {
        int otp = secureRandom.nextInt(900000) + 100000; // Range: 100000-999999
        return String.valueOf(otp);
    }

    /**
     * Store OTP in cache with key
     */
    public void storeOtp(String key, String otp) {
        Cache cache = cacheManager.getCache(OTP_CACHE_NAME);
        if (cache != null) {
            cache.put(key, otp);
            log.debug("Stored OTP in cache with key: {}", key);
        } else {
            log.error("Cache '{}' not found", OTP_CACHE_NAME);
        }
    }

    /**
     * Retrieve OTP from cache by key
     * Returns null if not found or expired
     */
    public String getOtp(String key) {
        Cache cache = cacheManager.getCache(OTP_CACHE_NAME);
        if (cache != null) {
            Cache.ValueWrapper wrapper = cache.get(key);
            if (wrapper != null) {
                String otp = (String) wrapper.get();
                log.debug("Retrieved OTP from cache with key: {}", key);
                return otp;
            }
        }
        log.debug("OTP not found in cache with key: {}", key);
        return null;
    }

    /**
     * Verify if the provided OTP matches the stored one
     */
    public boolean verifyOtp(String key, String providedOtp) {
        String storedOtp = getOtp(key);
        boolean isValid = storedOtp != null && storedOtp.equals(providedOtp);
        log.debug("OTP verification for key '{}': {}", key, isValid);
        return isValid;
    }

    /**
     * Clear OTP from cache after successful verification
     */
    public void clearOtp(String key) {
        Cache cache = cacheManager.getCache(OTP_CACHE_NAME);
        if (cache != null) {
            cache.evict(key);
            log.debug("Cleared OTP from cache with key: {}", key);
        }
    }

    /**
     * Generate and store OTP in one operation
     */
    public String generateAndStoreOtp(String key) {
        String otp = generateOtp();
        storeOtp(key, otp);
        return otp;
    }
}
