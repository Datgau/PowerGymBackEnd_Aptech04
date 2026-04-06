package com.example.project_backend04.service;

import com.example.project_backend04.config.BankPaymentConfig;
import com.example.project_backend04.dto.request.ApplyPromotionRequest;
import com.example.project_backend04.dto.request.BankPayment.SepayWebhookRequest;
import com.example.project_backend04.dto.response.ApplyPromotionResponse;
import com.example.project_backend04.dto.response.BankPayment.CreateBankPaymentResponse;
import com.example.project_backend04.dto.response.BankPayment.PaymentStatusResponse;
import com.example.project_backend04.dto.response.Service.GymServiceResponse;
import com.example.project_backend04.dto.response.Service.ServiceRegistrationResponse;
import com.example.project_backend04.dto.response.User.UserResponse;
import com.example.project_backend04.entity.GymService;
import com.example.project_backend04.entity.PaymentOrder;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.entity.TrainerBooking;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.BookingStatus;
import com.example.project_backend04.enums.PaymentStatus;
import com.example.project_backend04.enums.RegistrationStatus;
import com.example.project_backend04.enums.RegistrationType;
import com.example.project_backend04.exception.BankPaymentException;
import com.example.project_backend04.exception.PaymentOrderNotFoundException;
import com.example.project_backend04.exception.ServiceNotActiveException;
import com.example.project_backend04.mapper.UserMapper;
import com.example.project_backend04.repository.*;
import com.example.project_backend04.util.DatabaseRetryUtil;
import com.example.project_backend04.event.EntityChangedEvent;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataAccessException;

import java.math.BigDecimal;
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
        private final TrainerBookingRepository trainerBookingRepository;
        private final EmailService emailService;
        private final ApplicationEventPublisher eventPublisher;
        private final GymServiceService gymServiceService;
        private final UserMapper userMapper;
        private final MembershipPackageRepository membershipPackageRepository;
        private final com.example.project_backend04.repository.MembershipRepository membershipRepository;
        private final PromotionService promotionService;
        private final RewardService rewardService;
        private final com.example.project_backend04.mapper.ServiceRegistrationMapper serviceRegistrationMapper;

        @Transactional
        public CreateBankPaymentResponse createBankPayment(Long userId, Long serviceId) {
            return createBankPayment(userId, serviceId, null);
        }

        @Transactional
        public CreateBankPaymentResponse createBankPayment(Long userId, Long serviceId, Long bookingId) {
            return createBankPayment(userId, serviceId, null, "SERVICE", bookingId);
        }

        @Transactional
        public CreateBankPaymentResponse createBankPayment(Long userId, Long serviceId, Long packageId, String itemType, Long bookingId) {
            return createBankPayment(userId, serviceId, packageId, itemType, bookingId, null);
        }

        @Transactional
        public CreateBankPaymentResponse createBankPayment(Long userId, Long serviceId, Long packageId, String itemType, Long bookingId, String promotionCode) {
            try {
                if (userId == null || userId <= 0) {
                    throw new BankPaymentException("Invalid user ID", "INVALID_USER_ID");
                }

                User user = DatabaseRetryUtil.executeWithRetry(
                    () -> userRepository.findById(userId)
                        .orElseThrow(() -> new EntityNotFoundException("User not found with ID: " + userId)),
                    "findUserById"
                );
                Long amount;
                String itemName;
                Long itemId;
                
                if ("MEMBERSHIP".equals(itemType) && packageId != null) {
                    com.example.project_backend04.entity.MembershipPackage membershipPackage = DatabaseRetryUtil.executeWithRetry(
                        () -> membershipPackageRepository.findById(packageId)
                            .orElseThrow(() -> new EntityNotFoundException("Membership package not found with ID: " + packageId)),
                        "findMembershipPackageById"
                    );

                    if (!membershipPackage.getIsActive()) {
                        throw new BankPaymentException("Membership package is not active", "PACKAGE_NOT_ACTIVE");
                    }
                    
                    itemId = packageId;
                    itemName = membershipPackage.getName();
                    amount = membershipPackage.getPrice().longValue();

                } else {
                    // Handle service payment
                    if (serviceId == null || serviceId <= 0) {
                        throw new BankPaymentException("Invalid service ID", "INVALID_SERVICE_ID");
                    }
                    
                    GymService service = DatabaseRetryUtil.executeWithRetry(
                        () -> serviceRepository.findById(serviceId)
                            .orElseThrow(() -> new EntityNotFoundException("Service not found with ID: " + serviceId)),
                        "findServiceById"
                    );

                    if (!service.getIsActive()) {
                        throw new ServiceNotActiveException(serviceId);
                    }

                    itemId = serviceId;
                    itemName = service.getName();
                    amount = service.getPrice().longValue();
                }

                // Apply promotion discount if promotionCode is provided
                Long originalAmount = amount;
                Long promotionId = null;
                if (promotionCode != null && !promotionCode.trim().isEmpty()) {
                    try {
                        ApplyPromotionRequest promotionRequest = new ApplyPromotionRequest();
                        promotionRequest.setPromotionCode(promotionCode);
                        promotionRequest.setOrderAmount(BigDecimal.valueOf(amount));
                        
                        ApplyPromotionResponse promotionResponse = promotionService.validateAndCalculatePromotion(userId, promotionRequest);
                        
                        if (promotionResponse.getSuccess()) {
                            amount = promotionResponse.getFinalAmount().longValue();
                            promotionId = promotionResponse.getPromotionId();

                        } else {
                            log.warn("Promotion validation failed - Code: {}, Reason: {}", promotionCode, promotionResponse.getMessage());
                            // Don't throw error, just proceed with original amount
                        }
                    } catch (Exception e) {
                        log.error("Error applying promotion - Code: {}, Error: {}", promotionCode, e.getMessage());
                        // Don't throw error, just proceed with original amount
                    }
                }

                long timestamp = System.currentTimeMillis();
                String content = "GYM_" + userId + "_" + timestamp;

                String qrUrl;
                try {
                    qrUrl = generateVietQRUrl(amount, content);
                } catch (Exception e) {

                    throw new BankPaymentException("Failed to generate QR code", "QR_GENERATION_FAILED", e);
                }
                LocalDateTime now = LocalDateTime.now();
                int expiryMinutes = bankPaymentConfig.paymentExpiryMinutes() != null
                    ? bankPaymentConfig.paymentExpiryMinutes() : 15;

                PaymentOrder order;
                String orderId;
                
                if (bookingId != null) {
                    TrainerBooking booking = DatabaseRetryUtil.executeWithRetry(
                        () -> trainerBookingRepository.findById(bookingId)
                            .orElseThrow(() -> new EntityNotFoundException("Trainer booking not found with ID: " + bookingId)),
                        "findTrainerBookingById"
                    );
                    
                    // Validate booking belongs to user
                    if (!booking.getUser().getId().equals(userId)) {
                        throw new BankPaymentException("Booking does not belong to user", "UNAUTHORIZED_BOOKING");
                    }
                    
                    // Check if booking already has a payment order
                    if (booking.getPaymentOrder() != null) {
                        // Update existing payment order with new QR code and content
                        order = booking.getPaymentOrder();
                        orderId = order.getId();

                        log.info("Updating existing PaymentOrder {} for booking {}", orderId, bookingId);

                        // Update QR code and content
                        order.setContent(content);
                        order.setQrCodeUrl(qrUrl);
                        order.setExpiredAt(now.plusMinutes(expiryMinutes));
                        
                    } else {
                        orderId = UUID.randomUUID().toString();
                        order = new PaymentOrder();
                        order.setId(orderId);
                        order.setAmount(amount);
                        order.setContent(content);
                        order.setStatus(PaymentStatus.PENDING);
                        order.setPaymentMethod("VIETQR");
                        order.setQrCodeUrl(qrUrl);
                        order.setUser(user);
                        order.setItemId(itemId.toString());
                        order.setItemName(itemName);
                        order.setItemType("TRAINER_BOOKING");
                        order.setCreatedAt(now);
                        order.setExpiredAt(now.plusMinutes(expiryMinutes));
                        order.setPromotionId(promotionId);
                        order.setPromotionCode(promotionCode);
                        
                        booking.setPaymentOrder(order);
                        DatabaseRetryUtil.executeWithRetry(
                            () -> trainerBookingRepository.save(booking),
                            "linkBookingToPayment"
                        );
                        
                        log.info("Created new PaymentOrder {} for booking {}", orderId, bookingId);
                    }
                } else {
                    order = new PaymentOrder();
                    orderId = UUID.randomUUID().toString();
                    order.setId(orderId);
                    order.setAmount(amount);
                    order.setContent(content);
                    order.setStatus(PaymentStatus.PENDING);
                    order.setPaymentMethod("VIETQR");
                    order.setQrCodeUrl(qrUrl);
                    order.setUser(user);
                    order.setItemId(itemId.toString());
                    order.setItemName(itemName);
                    order.setItemType(itemType != null ? itemType : "SERVICE");
                    order.setCreatedAt(now);
                    order.setExpiredAt(now.plusMinutes(expiryMinutes));
                    order.setPromotionId(promotionId);
                    order.setPromotionCode(promotionCode);
                }

                // Save PaymentOrder with retry logic
                PaymentOrder savedOrder = DatabaseRetryUtil.executeWithRetry(
                    () -> paymentOrderRepository.save(order),
                    "savePaymentOrder"
                );

                log.info("AUDIT_LOG - PAYMENT_CREATION_SUCCESS - UserId: {}, ServiceId: {}, PackageId: {}, OrderId: {}, Amount: {}, Result: SUCCESS, Timestamp: {}",
                    userId, serviceId, packageId, savedOrder.getId(), savedOrder.getAmount(), LocalDateTime.now());

                return CreateBankPaymentResponse.builder()
                        .qrUrl(qrUrl)
                        .amount(amount)
                        .content(content)
                        .expiredAt(savedOrder.getExpiredAt())
                        .orderId(orderId)
                        .build();

            } catch (EntityNotFoundException e) {
                log.error("AUDIT_LOG - PAYMENT_CREATION_FAILURE - UserId: {}, ServiceId: {}, PackageId: {}, Result: ENTITY_NOT_FOUND, Error: {}, Timestamp: {}",
                    userId, serviceId, packageId, e.getMessage(), LocalDateTime.now());
                throw e;

            } catch (ServiceNotActiveException e) {
                log.error("AUDIT_LOG - PAYMENT_CREATION_FAILURE - UserId: {}, ServiceId: {}, PackageId: {}, Result: SERVICE_NOT_ACTIVE, Error: {}, Timestamp: {}",
                    userId, serviceId, packageId, e.getMessage(), LocalDateTime.now());
                throw e;

            } catch (BankPaymentException e) {
                log.error("AUDIT_LOG - PAYMENT_CREATION_FAILURE - UserId: {}, ServiceId: {}, PackageId: {}, Result: BANK_PAYMENT_ERROR, Error: {}, Timestamp: {}",
                    userId, serviceId, packageId, e.getMessage(), LocalDateTime.now());
                // Re-throw other BankPaymentException as-is
                throw e;

            } catch (DataAccessException e) {
                log.error("AUDIT_LOG - PAYMENT_CREATION_FAILURE - UserId: {}, ServiceId: {}, PackageId: {}, Result: DATABASE_ERROR, Error: {}, Timestamp: {}",
                    userId, serviceId, packageId, e.getMessage(), LocalDateTime.now());
                throw new BankPaymentException("Database operation failed", "DATABASE_ERROR", e);

            } catch (Exception e) {
                log.error("AUDIT_LOG - PAYMENT_CREATION_FAILURE - UserId: {}, ServiceId: {}, PackageId: {}, Result: UNEXPECTED_ERROR, Error: {}, Timestamp: {}",
                    userId, serviceId, packageId, e.getMessage(), LocalDateTime.now());
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

        @Transactional(isolation = org.springframework.transaction.annotation.Isolation.SERIALIZABLE)
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

                    // Update promotion usage count if promotion was used
                    if (updatedOrder.getPromotionId() != null) {
                        try {
                            log.info("Incrementing usage count for promotion ID: {}", updatedOrder.getPromotionId());
                            promotionService.incrementUsageCount(updatedOrder.getPromotionId(), updatedOrder.getUser().getId());
                            log.info("Promotion usage count incremented successfully for promotion ID: {}", updatedOrder.getPromotionId());
                        } catch (Exception e) {
                            log.error("Failed to increment promotion usage count for promotion ID: {} - continuing with payment processing", 
                                updatedOrder.getPromotionId(), e);
                        }
                    }

                    // Activate service for user
                    try {
                        activateService(updatedOrder);
                        sendPaymentConfirmationEmail(updatedOrder, payload);
                        
                        // Earn reward points for the payment
                        try {
                            Long userId = updatedOrder.getUser().getId();
                            BigDecimal amount = BigDecimal.valueOf(updatedOrder.getAmount());
                            String paymentId = updatedOrder.getId();
                            
                            log.info("Earning reward points for payment - UserId: {}, Amount: {}, PaymentId: {}", 
                                userId, amount, paymentId);
                            
                            rewardService.earnPoints(userId, amount, paymentId);
                            
                            log.info("Reward points earned successfully for payment {}", paymentId);
                            
                        } catch (Exception e) {
                            log.error("Failed to earn reward points for payment {} - continuing", 
                                updatedOrder.getId(), e);
                        }

                    } catch (Exception e) {
                        log.error("Failed to activate service for payment order {} - continuing with payment processing",
                            order.getId(), e);
                    }
                    log.info("AUDIT_LOG - WEBHOOK_PROCESSING_COMPLETE - Description: {}, Amount: {}, OrderId: {}, FinalStatus: SUCCESS, Result: PAYMENT_CONFIRMED, Timestamp: {}",
                        payload.getDescription(), payload.getAmount(), order.getId(), LocalDateTime.now());
                } else {
                    log.warn("AUDIT_LOG - WEBHOOK_PROCESSING_COMPLETE - Description: {}, Amount: {}, OrderId: {}, FinalStatus: PENDING, Result: AMOUNT_MISMATCH, ExpectedAmount: {}, ReceivedAmount: {}, Timestamp: {}",
                        payload.getDescription(), payload.getAmount(), order.getId(), order.getAmount(), payload.getAmount(), LocalDateTime.now());
                }

            } catch (DataAccessException e) {
                log.error("AUDIT_LOG - WEBHOOK_PROCESSING_FAILURE - Description: {}, Amount: {}, Result: DATABASE_ERROR, Error: {}, Timestamp: {}",
                    payload.getDescription(), payload.getAmount(), e.getMessage(), LocalDateTime.now());
                throw new BankPaymentException("Database error processing webhook", "DATABASE_ERROR", e);

            } catch (Exception e) {
                log.error("AUDIT_LOG - WEBHOOK_PROCESSING_FAILURE - Description: {}, Amount: {}, Result: UNEXPECTED_ERROR, Error: {}, Timestamp: {}",
                    payload.getDescription(), payload.getAmount(), e.getMessage(), LocalDateTime.now());
                throw new BankPaymentException("Unexpected error processing webhook", "WEBHOOK_PROCESSING_ERROR", e);
            }
        }

        public PaymentStatusResponse checkPaymentStatus(String content) {
            log.info("AUDIT_LOG - STATUS_CHECK_REQUEST - Content: {}, Timestamp: {}",
                content, LocalDateTime.now());

            try {
                if (content == null || content.trim().isEmpty()) {
                    log.error("Invalid content provided for status check: {}", content);
                    throw new BankPaymentException("Content cannot be null or empty", "INVALID_CONTENT");
                }
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
                log.info("AUDIT_LOG - STATUS_CHECK_RESPONSE - Content: {}, OrderId: {}, Status: {}, Amount: {}, Result: SUCCESS, Timestamp: {}",
                    content, order.getId(), order.getStatus(), order.getAmount(), LocalDateTime.now());

                return response;

            } catch (PaymentOrderNotFoundException e) {
                throw e;

            } catch (DataAccessException e) {
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
                String itemType = order.getItemType();
                
                if ("MEMBERSHIP".equals(itemType)) {
                    activateMembership(order);
                } else {
                    activateServiceRegistration(order);
                }
            } catch (BankPaymentException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to activate service/membership for user: {} - OrderId: {}, ItemId: {}",
                    order.getUser().getId(), order.getId(), order.getItemId(), e);
                throw new BankPaymentException("Service/Membership activation failed", "ACTIVATION_FAILED", e);
            }
        }
        
        // Activate membership after successful payment
        private void activateMembership(PaymentOrder order) {
            Long userId = order.getUser().getId();
            Long packageId = Long.parseLong(order.getItemId());
            
            log.info("Activating membership for user: {} - OrderId: {}, PackageId: {}", 
                userId, order.getId(), packageId);
            
            User user = DatabaseRetryUtil.executeWithRetry(
                () -> userRepository.findById(userId)
                    .orElseThrow(() -> new BankPaymentException(
                        "User not found during membership activation: " + userId, "USER_NOT_FOUND")),
                "findUserForMembershipActivation"
            );
            
            com.example.project_backend04.entity.MembershipPackage membershipPackage = DatabaseRetryUtil.executeWithRetry(
                () -> membershipPackageRepository.findById(packageId)
                    .orElseThrow(() -> new BankPaymentException(
                        "Membership package not found during activation: " + packageId, "PACKAGE_NOT_FOUND")),
                "findPackageForActivation"
            );
            
            java.time.LocalDate startDate = java.time.LocalDate.now();
            java.time.LocalDate endDate = startDate.plusDays(membershipPackage.getDuration());
            
            com.example.project_backend04.entity.Membership membership = new com.example.project_backend04.entity.Membership();
            membership.setUser(user);
            membership.setMembershipPackage(membershipPackage);
            membership.setStartDate(startDate);
            membership.setEndDate(endDate);
            membership.setPaidAmount(membershipPackage.getPrice());
            membership.setPaymentMethod(com.example.project_backend04.entity.Membership.PaymentMethod.TRANSFER);
            membership.setStatus(com.example.project_backend04.entity.Membership.MembershipStatus.ACTIVE);
            membership.setOrderId(order.getId());
            membership.setNotes("Activated via bank transfer payment. OrderId: " + order.getId());
            
            com.example.project_backend04.entity.Membership savedMembership = DatabaseRetryUtil.executeWithRetry(
                () -> membershipRepository.save(membership),
                "saveMembership"
            );
            
            log.info("Membership {} activated for userId={}, packageId={}, orderId={}, startDate={}, endDate={}",
                savedMembership.getId(), userId, packageId, order.getId(), startDate, endDate);
        }
        
        // Activate service registration after successful payment
        private void activateServiceRegistration(PaymentOrder order) {
            try {
                // For SERVICE itemType, activate ServiceRegistration
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

                // Find PENDING ServiceRegistration for this user and service
                Optional<ServiceRegistration> pendingReg = serviceRegistrationRepository
                    .findByUserAndGymServiceAndStatus(user, service, RegistrationStatus.PENDING);

                ServiceRegistration activatedRegistration = null;
                
                if (pendingReg.isPresent()) {
                    // Activate existing PENDING registration
                    ServiceRegistration registration = pendingReg.get();
                    registration.setStatus(RegistrationStatus.ACTIVE);
                    registration.setNotes("Activated via bank transfer payment. OrderId: " + order.getId());
                    registration.setPaymentOrder(order); // Link payment order
                    
                    activatedRegistration = DatabaseRetryUtil.executeWithRetry(
                        () -> serviceRegistrationRepository.save(registration),
                        "activateServiceRegistration"
                    );
                    
                    log.info("ServiceRegistration {} activated for userId={}, serviceId={}, orderId={}",
                        registration.getId(), userId, serviceId, order.getId());
                } else {
                    // Check if already ACTIVE
                    boolean alreadyActive = serviceRegistrationRepository
                        .existsByUserAndGymServiceAndStatus(user, service, RegistrationStatus.ACTIVE);

                    if (alreadyActive) {
                        log.warn("ServiceRegistration already ACTIVE for userId={}, serviceId={} - skipping",
                            userId, serviceId);
                        return;
                    }

                    // Create new ACTIVE registration (fallback for old flow)
                    ServiceRegistration registration = new ServiceRegistration();
                    registration.setUser(user);
                    registration.setGymService(service);
                    registration.setStatus(RegistrationStatus.ACTIVE);
                    registration.setRegistrationType(RegistrationType.ONLINE);
                    registration.setNotes("Activated via bank transfer payment. OrderId: " + order.getId());
                    registration.setPaymentOrder(order); // Link payment order

                    activatedRegistration = DatabaseRetryUtil.executeWithRetry(
                        () -> serviceRegistrationRepository.save(registration),
                        "saveServiceRegistration"
                    );

                    log.info("ServiceRegistration created successfully for userId={}, serviceId={}, orderId={}",
                        userId, serviceId, order.getId());
                }
                
                // Emit event for activated service registration
                if (activatedRegistration != null) {
                    ServiceRegistrationResponse response = mapToResponse(activatedRegistration);
                    eventPublisher.publishEvent(
                        new EntityChangedEvent(this, "SERVICE_REGISTRATION", "ACTIVATED", response, activatedRegistration.getId())
                    );
                    log.info("Published SERVICE_REGISTRATION ACTIVATED event for registration {}", activatedRegistration.getId());
                }

            } catch (BankPaymentException e) {
                throw e;
            } catch (Exception e) {
                log.error("Failed to activate service for user: {} - OrderId: {}, ItemId: {}",
                    order.getUser().getId(), order.getId(), order.getItemId(), e);
                throw new BankPaymentException("Service activation failed", "SERVICE_ACTIVATION_FAILED", e);
            }
        }
        private void sendPaymentConfirmationEmail(PaymentOrder order, SepayWebhookRequest payload) {
            try {
                String userEmail = order.getUser().getEmail();
                String fullName = order.getUser().getFullName();
                String transactionId = payload.getTransactionId();
                String serviceName = order.getItemName();
                String amount = String.format("%,d", order.getAmount());

                // Format payment time from webhook
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
                String paymentTime = LocalDateTime.now().format(formatter);

                log.info("Sending payment confirmation email to {} for order {}", userEmail, order.getId());

                // Send email asynchronously
                emailService.sendPaymentConfirmationEmailAsync(
                    userEmail,
                    fullName,
                    transactionId,
                    serviceName,
                    amount,
                    paymentTime
                ).thenAccept(success -> {
                    if (success) {
                        log.info("Payment confirmation email sent successfully to {} for order {}",
                            userEmail, order.getId());
                    } else {
                        log.error("Failed to send payment confirmation email to {} for order {}",
                            userEmail, order.getId());
                    }
                });

            } catch (Exception e) {
                log.error("Error sending payment confirmation email for order {} - continuing",
                    order.getId(), e);
                // Don't fail the payment processing if email fails
            }
        }
        
        // Map ServiceRegistration to ServiceRegistrationResponse
        private ServiceRegistrationResponse mapToResponse(ServiceRegistration registration) {
            return serviceRegistrationMapper.toResponse(registration);
        }



}
