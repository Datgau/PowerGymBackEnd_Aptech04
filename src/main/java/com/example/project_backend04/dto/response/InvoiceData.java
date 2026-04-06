package com.example.project_backend04.dto.response;

import com.example.project_backend04.entity.PaymentOrder;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceData {
    
    // Invoice Info
    private String invoiceNumber;
    private String invoiceDate;
    private String paymentMethod;
    private String paymentStatus;
    
    // Customer Info
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    
    // Item Info
    private String itemType;
    private String itemName;
    private String itemId;
    
    // Payment Info
    private Long amount;
    private String amountFormatted;
    private String transactionRef;
    private String momoTransId;
    
    // Promotion Info
    private String promotionCode;
    private Long promotionId;
    
    // Additional Info
    private String content;
    private String createdAt;
    private String expiredAt;
    
    // Company Info (static)
    private String companyName = "PowerGym Fitness Center";
    private String companyAddress = "123 Fitness Street, Ho Chi Minh City";
    private String companyPhone = "+84 123 456 789";
    private String companyEmail = "info@powergym.vn";
    
    /**
     * Create InvoiceData from PaymentOrder
     */
    public static InvoiceData fromPaymentOrder(PaymentOrder paymentOrder) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        
        return InvoiceData.builder()
            // Invoice Info
            .invoiceNumber(paymentOrder.getId())
            .invoiceDate(paymentOrder.getCreatedAt() != null 
                ? paymentOrder.getCreatedAt().format(dateFormatter) 
                : "")
            .paymentMethod(paymentOrder.getPaymentMethod() != null 
                ? paymentOrder.getPaymentMethod() 
                : "Bank Transfer")
            .paymentStatus(paymentOrder.getStatus() != null 
                ? paymentOrder.getStatus().name() 
                : "UNKNOWN")
            
            // Customer Info
            .customerName(paymentOrder.getUser() != null 
                ? paymentOrder.getUser().getFullName() 
                : "N/A")
            .customerEmail(paymentOrder.getUser() != null 
                ? paymentOrder.getUser().getEmail() 
                : "N/A")
            .customerPhone(paymentOrder.getUser() != null 
                ? paymentOrder.getUser().getPhoneNumber() 
                : "N/A")
            
            // Item Info
            .itemType(paymentOrder.getItemType())
            .itemName(paymentOrder.getItemName())
            .itemId(paymentOrder.getItemId())
            
            // Payment Info
            .amount(paymentOrder.getAmount())
            .amountFormatted(formatAmount(paymentOrder.getAmount()))
            .transactionRef(paymentOrder.getTransactionRef())
            .momoTransId(paymentOrder.getMomoTransId())
            
            // Promotion Info
            .promotionCode(paymentOrder.getPromotionCode())
            .promotionId(paymentOrder.getPromotionId())
            
            // Additional Info
            .content(paymentOrder.getContent())
            .createdAt(paymentOrder.getCreatedAt() != null 
                ? paymentOrder.getCreatedAt().format(dateFormatter) 
                : "")
            .expiredAt(paymentOrder.getExpiredAt() != null 
                ? paymentOrder.getExpiredAt().format(dateFormatter) 
                : "")
            
            .build();
    }
    
    /**
     * Format amount to Vietnamese currency format
     */
    private static String formatAmount(Long amount) {
        if (amount == null) {
            return "0 VNĐ";
        }
        return String.format("%,d VNĐ", amount);
    }
}
