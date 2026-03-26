package com.example.project_backend04.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class MoMoUtils {
    
    private static final String HMAC_SHA256 = "HmacSHA256";
    public static String generateSignature(String rawData, String secretKey) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hash = mac.doFinal(rawData.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Error generating MoMo signature", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    public static String generatePaymentSignature(String accessKey, Long amount, String extraData, 
                                                 String ipnUrl, String orderId, String orderInfo, 
                                                 String partnerCode, String redirectUrl, 
                                                 String requestId, String requestType, String secretKey) {
        String rawData = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&ipnUrl=" + ipnUrl +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&partnerCode=" + partnerCode +
                "&redirectUrl=" + redirectUrl +
                "&requestId=" + requestId +
                "&requestType=" + requestType;
        
        return generateSignature(rawData, secretKey);
    }

    public static String generateIPNSignature(String accessKey, Long amount, String extraData, 
                                            String message, String orderId, String orderInfo, 
                                            String orderType, String partnerCode, String payType, 
                                            String requestId, Long responseTime, Integer resultCode, 
                                            Long transId, String secretKey) {
        String rawData = "accessKey=" + accessKey +
                "&amount=" + amount +
                "&extraData=" + extraData +
                "&message=" + message +
                "&orderId=" + orderId +
                "&orderInfo=" + orderInfo +
                "&orderType=" + orderType +
                "&partnerCode=" + partnerCode +
                "&payType=" + payType +
                "&requestId=" + requestId +
                "&responseTime=" + responseTime +
                "&resultCode=" + resultCode +
                "&transId=" + transId;
        
        return generateSignature(rawData, secretKey);
    }

    public static String encodeBase64(String data) {
        return Base64.getEncoder().encodeToString(data.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeBase64(String encodedData) {
        return new String(Base64.getDecoder().decode(encodedData), StandardCharsets.UTF_8);
    }
}