package com.example.project_backend04.service;

import com.example.project_backend04.config.BankPaymentConfig;
import com.example.project_backend04.dto.request.BankPayment.SepayWebhookRequest;
import com.example.project_backend04.dto.response.BankPayment.CreateBankPaymentResponse;
import com.example.project_backend04.dto.response.BankPayment.PaymentStatusResponse;
import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.PaymentOrder;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.PaymentStatus;
import com.example.project_backend04.exception.BankPaymentException;
import com.example.project_backend04.exception.PaymentOrderNotFoundException;
import com.example.project_backend04.exception.ServiceNotActiveException;
import com.example.project_backend04.repository.GymServiceRepository;
import com.example.project_backend04.repository.PaymentOrderRepository;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.repository.UserRepository;
import com.example.project_backend04.util.DatabaseRetryUtil;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankPaymentService {

        private final PaymentOrderRepository paymentOrderRepository;
        private final GymServiceRepository serviceRepository;
        private final UserRepository userRepository;
        private final BankPaymentConfig bankPaymentConfig;
        private final WebhookValidationService webhookValidationService;
        private final ServiceRegistrationRepository serviceRegistrationRepository;

        @Transactional
        public CreateBankPaymentResponse createBankPayment(Long userId, Long serviceId) {
            // Audit log: Payment creation attempt
            log.info("AUDIT_LOG - PAYMENT_CREATION_ATTEMPT - UserId: {}, ServiceId: {}, Timestamp: {}", 
                userId, serviceId, LocalDateTime.now());
            
            try {
                // Validate input parameters
                if (userId == null || userId <= 0) {
                    log.error("Invalid userId provided: {}", userId);
                    throw new BankPaymentException("Invalid user ID", "INVALID_USER_ID");
                }
                
                if (serviceId == null || serviceId <= 0) {
                    log.error("Invalid serviceId provided: {}", serviceId);
                    throw new BankPaymentException("Invalid service ID", "INVALID_SERVICE_ID");
                }

                // Validate user existence with retry logic
                User user = DatabaseRetryUtil.executeWithRetry(
                    () -> userRepository.findById(userId)
                        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId)),
                    "findUserById"
                );
                log.debug("User found - ID: {}, Name: {}", user.getId(), user.getFullName());

                // Validate service existence with retry logic
                GymService service = DatabaseRetryUtil.executeWithRetry(
                    () -> serviceRepository.findById(serviceId)
                        .orElseThrow(() -> new EntityNotFoundException("Service not found with ID: " + serviceId)),
                    "findServiceById"
                );
                log.debug("Service found - ID: {}, Name: {}, Price: {}, Active: {}", 
                    service.getId(), service.getName(), service.getPrice(), service.getIsActive());

                // Validate service is active
                if (!service.getIsActive()) {
                    log.warn("Attempted to create payment for inactive service - ServiceId: {}", serviceId);
                    throw new ServiceNotActiveException(serviceId);
                }

                // Generate unique payment content with format "GYM_{userId}_{timestamp}"
                long timestamp = System.currentTimeMillis();
                String content = "GYM_" + userId + "_" + timestamp;
                log.debug("Generated payment content: {}", content);

                // Create VietQR URL with configured bank details
                String qrUrl;
                try {
                    qrUrl = generateVietQRUrl(service.getPrice().longValue(), content);
                    log.debug("Generated VietQR URL: {}", qrUrl);
                } catch (Exception e) {
                    log.error("Failed to generate VietQR URL - ServiceId: {}, Amount: {}, Content: {}", 
                        serviceId, service.getPrice(), content, e);
                    throw new BankPaymentException("Failed to generate QR code", "QR_GENERATION_FAILED", e);
                }

                // Generate unique payment order ID
                String orderId = UUID.randomUUID().toString();
                log.debug("Generated order ID: {}", orderId);

                // Set expiredAt từ config
                LocalDateTime now = LocalDateTime.now();
                int expiryMinutes = bankPaymentConfig.paymentExpiryMinutes() != null
                    ? bankPaymentConfig.paymentExpiryMinutes() : 15;

                // Create PaymentOrder with all required fields
                PaymentOrder order = new PaymentOrder();
                order.setId(orderId);
                order.setAmount(service.getPrice().longValue());
                order.setContent(content);
                order.setStatus(PaymentStatus.PENDING);
                order.setPaymentMethod("VIETQR");
                order.setQrCodeUrl(qrUrl);
                order.setUser(user);
                order.setItemId(serviceId.toString());
                order.setItemName(service.getName());
                order.setItemType("SERVICE");
                order.setCreatedAt(now);
                order.setExpiredAt(now.plusMinutes(expiryMinutes));

                // Save PaymentOrder with retry logic
                PaymentOrder savedOrder = DatabaseRetryUtil.executeWithRetry(
                    () -> paymentOrderRepository.save(order),
                    "savePaymentOrder"
                );
                
                // Audit log: Payment creation success
                log.info("AUDIT_LOG - PAYMENT_CREATION_SUCCESS - UserId: {}, ServiceId: {}, OrderId: {}, Amount: {}, Result: SUCCESS, Timestamp: {}", 
                    userId, serviceId, savedOrder.getId(), savedOrder.getAmount(), LocalDateTime.now());

                // Return CreateBankPaymentResponse with payment details
                return CreateBankPaymentResponse.builder()
                        .qrUrl(qrUrl)
                        .amount(service.getPrice().longValue())
                        .content(content)
                        .expiredAt(savedOrder.getExpiredAt())
                        .orderId(orderId)
                        .build();
                        
            } catch (EntityNotFoundException e) {
                // Audit log: Payment creation failure
                log.error("AUDIT_LOG - PAYMENT_CREATION_FAILURE - UserId: {}, ServiceId: {}, Result: ENTITY_NOT_FOUND, Error: {}, Timestamp: {}", 
                    userId, serviceId, e.getMessage(), LocalDateTime.now());
                throw e; // Will be handled by GlobalExceptionHandler
                
            } catch (ServiceNotActiveException e) {
                // Audit log: Payment creation failure
                log.error("AUDIT_LOG - PAYMENT_CREATION_FAILURE - UserId: {}, ServiceId: {}, Result: SERVICE_NOT_ACTIVE, Error: {}, Timestamp: {}", 
                    userId, serviceId, e.getMessage(), LocalDateTime.now());
                // Re-throw ServiceNotActiveException as-is (it extends BankPaymentException)
                throw e;
                
            } catch (BankPaymentException e) {
                // Audit log: Payment creation failure
                log.error("AUDIT_LOG - PAYMENT_CREATION_FAILURE - UserId: {}, ServiceId: {}, Result: BANK_PAYMENT_ERROR, Error: {}, Timestamp: {}", 
                    userId, serviceId, e.getMessage(), LocalDateTime.now());
                // Re-throw other BankPaymentException as-is
                throw e;
                
            } catch (DataAccessException e) {
                // Audit log: Payment creation failure
                log.error("AUDIT_LOG - PAYMENT_CREATION_FAILURE - UserId: {}, ServiceId: {}, Result: DATABASE_ERROR, Error: {}, Timestamp: {}", 
                    userId, serviceId, e.getMessage(), LocalDateTime.now());
                throw new BankPaymentException("Database operation failed", "DATABASE_ERROR", e);
                
            } catch (Exception e) {
                // Audit log: Payment creation failure
                log.error("AUDIT_LOG - PAYMENT_CREATION_FAILURE - UserId: {}, ServiceId: {}, Result: UNEXPECTED_ERROR, Error: {}, Timestamp: {}", 
                    userId, serviceId, e.getMessage(), LocalDateTime.now());
                throw new BankPaymentException("Unexpected error occurred", "UNEXPECTED_ERROR", e);
            }
        }

        /**
         * Generate VietQR URL with configured bank details
         */
        private String generateVietQRUrl(Long amount, String content) {
            try {
                log.debug("Generating VietQR URL - Amount: {}, Content: {}", amount, content);
                
                // Validate configuration
                if (bankPaymentConfig.bankCode() == null || bankPaymentConfig.bankCode().trim().isEmpty()) {
                    throw new BankPaymentException("Bank code not configured", "MISSING_CONFIG");
                }
                
                if (bankPaymentConfig.accountNo() == null || bankPaymentConfig.accountNo().trim().isEmpty()) {
                    throw new BankPaymentException("Account number not configured", "MISSING_CONFIG");
                }
                
                if (bankPaymentConfig.accountName() == null || bankPaymentConfig.accountName().trim().isEmpty()) {
                    throw new BankPaymentException("Account name not configured", "MISSING_BANK_CONFIG");
                }
                
                // Validate input parameters
                if (amount == null || amount <= 0) {
                    throw new BankPaymentException("Invalid amount for QR generation: " + amount, "INVALID_AMOUNT");
                }
                
                if (content == null || content.trim().isEmpty()) {
                    throw new BankPaymentException("Invalid content for QR generation", "INVALID_CONTENT");
                }
                
                // Format: https://img.vietqr.io/image/{bankCode}-{accountNo}-compact2.png?amount={amount}&addInfo={content}&accountName={accountName}
                String url = bankPaymentConfig.vietqrBaseUrl() + "/" 
                        + bankPaymentConfig.bankCode() + "-" 
                        + bankPaymentConfig.accountNo() + "-compact2.png"
                        + "?amount=" + amount 
                        + "&addInfo=" + content
                        + "&accountName=" + bankPaymentConfig.accountName();
                        
                log.debug("Generated VietQR URL successfully");
                return url;
                
            } catch (BankPaymentException e) {
                throw e;
            } catch (Exception e) {
                log.error("Unexpected error generating VietQR URL - Amount: {}, Content: {}", amount, content, e);
                throw new BankPaymentException("Failed to generate VietQR URL", "QR_GENERATION_ERROR", e);
            }
        }

        @Transactional
        public void handleSepayWebhook(SepayWebhookRequest payload) {
            // Audit log: Webhook processing attempt
            log.info("AUDIT_LOG - WEBHOOK_PROCESSING_ATTEMPT - Description: {}, Amount: {}, TransactionId: {}, TransferType: {}, Timestamp: {}", 
                payload.getDescription(), payload.getAmount(), payload.getTransactionId(), payload.getTransferType(), LocalDateTime.now());
            
            try {
                // Ignore outgoing or reversal transactions
                if (!"in".equalsIgnoreCase(payload.getTransferType())) {
                    log.info("AUDIT_LOG - WEBHOOK_PROCESSING_SKIPPED - Description: {}, TransferType: {} - Ignoring non-incoming transaction", 
                        payload.getDescription(), payload.getTransferType());
                    return;
                }

                // Enhanced webhook payload validation and sanitization
                webhookValidationService.validateWebhookPayload(payload);
                log.debug("Webhook payload validation completed successfully");

                // Find PaymentOrder by description content
                Optional<PaymentOrder> orderOpt = DatabaseRetryUtil.executeWithRetry(
                    () -> {
                        String rawDescription = payload.getDescription();
                        if (rawDescription == null) return Optional.empty();
                        
                        Optional<PaymentOrder> exactMatch = paymentOrderRepository.findByContent(rawDescription);
                        if (exactMatch.isPresent()) return exactMatch;
                        
                        String normalizedDesc = rawDescription.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                        java.util.List<PaymentOrder> pendingOrders = paymentOrderRepository.findByStatus(PaymentStatus.PENDING);
                        
                        return pendingOrders.stream()
                            .filter(o -> {
                                if (o.getContent() == null) return false;
                                String normalizedOrderContent = o.getContent().replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
                                return normalizedDesc.contains(normalizedOrderContent);
                            })
                            .findFirst();
                    },
                    "findPaymentOrderByContentRobust"
                );
                
                // Skip processing if order not found
                if (orderOpt.isEmpty()) {
                    // Audit log: Webhook processing - order not found
                    log.info("AUDIT_LOG - WEBHOOK_PROCESSING_COMPLETE - Description: {}, Amount: {}, OrderId: NOT_FOUND, FinalStatus: SKIPPED, Result: ORDER_NOT_FOUND, Timestamp: {}", 
                        payload.getDescription(), payload.getAmount(), LocalDateTime.now());
                    return;
                }
                
                PaymentOrder order = orderOpt.get();
                log.debug("Found payment order - ID: {}, Status: {}, Amount: {}, ExpiredAt: {}", 
                    order.getId(), order.getStatus(), order.getAmount(), order.getExpiredAt());

                // Skip processing if order already PAID (SUCCESS)
                if (order.getStatus() == PaymentStatus.SUCCESS) {
                    // Audit log: Webhook processing - already paid
                    log.info("AUDIT_LOG - WEBHOOK_PROCESSING_COMPLETE - Description: {}, Amount: {}, OrderId: {}, FinalStatus: SUCCESS, Result: ALREADY_PAID, Timestamp: {}", 
                        payload.getDescription(), payload.getAmount(), order.getId(), LocalDateTime.now());
                    return;
                }

                // Handle expired payments by setting status to FAILED
                if (order.getExpiredAt() != null && order.getExpiredAt().isBefore(LocalDateTime.now())) {
                    log.warn("Payment order {} has expired, updating status to FAILED", order.getId());
                    
                    order.setStatus(PaymentStatus.FAILED);
                    DatabaseRetryUtil.executeWithRetry(
                        () -> paymentOrderRepository.save(order),
                        "updateExpiredPaymentOrder"
                    );
                    
                    // Audit log: Webhook processing - expired payment
                    log.info("AUDIT_LOG - WEBHOOK_PROCESSING_COMPLETE - Description: {}, Amount: {}, OrderId: {}, FinalStatus: FAILED, Result: PAYMENT_EXPIRED, Timestamp: {}", 
                        payload.getDescription(), payload.getAmount(), order.getId(), LocalDateTime.now());
                    return;
                }

                // Update status to PAID (SUCCESS) when amount matches and activate service
                if (order.getAmount().intValue() == payload.getAmount()) {
                    log.info("Payment amounts match, updating order {} to SUCCESS status", order.getId());
                    
                    order.setStatus(PaymentStatus.SUCCESS);
                    PaymentOrder updatedOrder = DatabaseRetryUtil.executeWithRetry(
                        () -> paymentOrderRepository.save(order),
                        "updateSuccessfulPaymentOrder"
                    );
                    
                    // Activate service for user
                    try {
                        activateService(updatedOrder);
                        log.info("Service activated successfully for payment order {}", order.getId());
                    } catch (Exception e) {
                        log.error("Failed to activate service for payment order {} - continuing with payment processing", 
                            order.getId(), e);
                        // Don't fail the webhook processing if service activation fails
                    }
                    
                    // Audit log: Webhook processing - successful payment
                    log.info("AUDIT_LOG - WEBHOOK_PROCESSING_COMPLETE - Description: {}, Amount: {}, OrderId: {}, FinalStatus: SUCCESS, Result: PAYMENT_CONFIRMED, Timestamp: {}", 
                        payload.getDescription(), payload.getAmount(), order.getId(), LocalDateTime.now());
                } else {
                    // Audit log: Webhook processing - amount mismatch
                    log.warn("AUDIT_LOG - WEBHOOK_PROCESSING_COMPLETE - Description: {}, Amount: {}, OrderId: {}, FinalStatus: PENDING, Result: AMOUNT_MISMATCH, ExpectedAmount: {}, ReceivedAmount: {}, Timestamp: {}", 
                        payload.getDescription(), payload.getAmount(), order.getId(), order.getAmount(), payload.getAmount(), LocalDateTime.now());
                }
                
            } catch (DataAccessException e) {
                // Audit log: Webhook processing failure
                log.error("AUDIT_LOG - WEBHOOK_PROCESSING_FAILURE - Description: {}, Amount: {}, Result: DATABASE_ERROR, Error: {}, Timestamp: {}", 
                    payload.getDescription(), payload.getAmount(), e.getMessage(), LocalDateTime.now());
                throw new BankPaymentException("Database error processing webhook", "DATABASE_ERROR", e);
                
            } catch (Exception e) {
                // Audit log: Webhook processing failure
                log.error("AUDIT_LOG - WEBHOOK_PROCESSING_FAILURE - Description: {}, Amount: {}, Result: UNEXPECTED_ERROR, Error: {}, Timestamp: {}", 
                    payload.getDescription(), payload.getAmount(), e.getMessage(), LocalDateTime.now());
                throw new BankPaymentException("Unexpected error processing webhook", "WEBHOOK_PROCESSING_ERROR", e);
            }
        }

        public PaymentStatusResponse checkPaymentStatus(String content) {
            // Audit log: Status check request
            log.info("AUDIT_LOG - STATUS_CHECK_REQUEST - Content: {}, Timestamp: {}", 
                content, LocalDateTime.now());
            
            try {
                // Validate input parameter
                if (content == null || content.trim().isEmpty()) {
                    log.error("Invalid content provided for status check: {}", content);
                    throw new BankPaymentException("Content cannot be null or empty", "INVALID_CONTENT");
                }
                
                // Find PaymentOrder by content with retry logic
                Optional<PaymentOrder> orderOpt = DatabaseRetryUtil.executeWithRetry(
                    () -> paymentOrderRepository.findByContent(content),
                    "findPaymentOrderByContentForStatus"
                );
                
                if (orderOpt.isEmpty()) {
                    // Audit log: Status check - order not found
                    log.warn("AUDIT_LOG - STATUS_CHECK_RESPONSE - Content: {}, Result: ORDER_NOT_FOUND, Timestamp: {}", 
                        content, LocalDateTime.now());
                    throw new PaymentOrderNotFoundException(content);
                }
                
                PaymentOrder order = orderOpt.get();
                log.debug("Payment order found - ID: {}, Status: {}, Amount: {}, CreatedAt: {}, ExpiredAt: {}", 
                    order.getId(), order.getStatus(), order.getAmount(), order.getCreatedAt(), order.getExpiredAt());
                
                PaymentStatusResponse response = PaymentStatusResponse.builder()
                        .status(order.getStatus())
                        .orderId(order.getId())
                        .amount(order.getAmount())
                        .createdAt(order.getCreatedAt())
                        .expiredAt(order.getExpiredAt())
                        .itemType(order.getItemType())
                        .itemName(order.getItemName())
                        .build();
                        
                // Audit log: Status check success
                log.info("AUDIT_LOG - STATUS_CHECK_RESPONSE - Content: {}, OrderId: {}, Status: {}, Amount: {}, Result: SUCCESS, Timestamp: {}", 
                    content, order.getId(), order.getStatus(), order.getAmount(), LocalDateTime.now());
                    
                return response;
                
            } catch (PaymentOrderNotFoundException e) {
                // Re-throw as-is (already logged above)
                throw e;
                
            } catch (DataAccessException e) {
                // Audit log: Status check failure
                log.error("AUDIT_LOG - STATUS_CHECK_RESPONSE - Content: {}, Result: DATABASE_ERROR, Error: {}, Timestamp: {}", 
                    content, e.getMessage(), LocalDateTime.now());
                throw new BankPaymentException("Database error checking payment status", "DATABASE_ERROR", e);
                
            } catch (Exception e) {
                // Audit log: Status check failure
                log.error("AUDIT_LOG - STATUS_CHECK_RESPONSE - Content: {}, Result: UNEXPECTED_ERROR, Error: {}, Timestamp: {}", 
                    content, e.getMessage(), LocalDateTime.now());
                throw new BankPaymentException("Unexpected error checking payment status", "STATUS_CHECK_ERROR", e);
            }
        }

        // Activate service after successful payment
        private void activateService(PaymentOrder order) {
            try {
                Long userId = order.getUser().getId();
                Long serviceId = Long.parseLong(order.getItemId());
                
                log.info("Activating service for user: {} - OrderId: {}, ServiceId: {}, ServiceName: {}", 
                    userId, order.getId(), serviceId, order.getItemName());

                // Load user
                User user = DatabaseRetryUtil.executeWithRetry(
                    () -> userRepository.findById(userId)
                        .orElseThrow(() -> new BankPaymentException(
                            "User not found during service activation: " + userId, "USER_NOT_FOUND")),
                    "findUserForActivation"
                );

                // Load service
                GymService service = DatabaseRetryUtil.executeWithRetry(
                    () -> serviceRepository.findById(serviceId)
                        .orElseThrow(() -> new BankPaymentException(
                            "Service not found during activation: " + serviceId, "SERVICE_NOT_FOUND")),
                    "findServiceForActivation"
                );

                // Kiễm tra nếu đã có registration ACTIVE rồi thì bỏ qua
                boolean alreadyActive = serviceRegistrationRepository
                    .existsByUserAndGymServiceAndStatus(
                        user, service, ServiceRegistration.RegistrationStatus.ACTIVE);
                
                if (alreadyActive) {
                    log.warn("ServiceRegistration already ACTIVE for userId={}, serviceId={} - skipping",
                        userId, serviceId);
                    return;
                }

                // Tạo ServiceRegistration mới
                ServiceRegistration registration = new ServiceRegistration();
                registration.setUser(user);
                registration.setGymService(service);
                registration.setStatus(ServiceRegistration.RegistrationStatus.ACTIVE);
                registration.setNotes("Activated via bank transfer payment. OrderId: " + order.getId());

                DatabaseRetryUtil.executeWithRetry(
                    () -> serviceRegistrationRepository.save(registration),
                    "saveServiceRegistration"
                );

                log.info("ServiceRegistration created successfully for userId={}, serviceId={}, orderId={}",
                    userId, serviceId, order.getId());

            } catch (BankPaymentException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to activate service for user: {} - OrderId: {}, ItemId: {}", 
                    order.getUser().getId(), order.getId(), order.getItemId(), e);
                throw new BankPaymentException("Service activation failed", "SERVICE_ACTIVATION_FAILED", e);
            }
        }

}
