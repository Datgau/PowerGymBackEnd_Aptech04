package com.example.project_backend04.service;

import com.example.project_backend04.dto.request.Payment.CreatePaymentRequest;
import com.example.project_backend04.dto.request.Payment.MoMoIPNRequest;
import com.example.project_backend04.dto.request.Payment.MoMoPaymentRequest;
import com.example.project_backend04.dto.response.Payment.MoMoPaymentResponse;
import com.example.project_backend04.dto.response.Payment.PaymentWithTrainerSelectionResponse;
import com.example.project_backend04.dto.response.Service.ServiceRegistrationWithTrainerSelectionResponse;
import com.example.project_backend04.entity.PaymentOrder;
import com.example.project_backend04.entity.ServiceRegistration;
import com.example.project_backend04.entity.User;
import com.example.project_backend04.enums.PaymentStatus;
import com.example.project_backend04.repository.PaymentOrderRepository;
import com.example.project_backend04.repository.ServiceRegistrationRepository;
import com.example.project_backend04.util.MoMoUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoMoPaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final ServiceRegistrationRepository serviceRegistrationRepository;
    private final ServiceRegistrationService serviceRegistrationService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${MOMO_PARTNER_CODE}")
    private String partnerCode;

    @Value("${MOMO_ACCESS_KEY}")
    private String accessKey;

    @Value("${MOMO_SECRET_KEY}")
    private String secretKey;

    @Value("${MOMO_ENDPOINT}")
    private String endpoint;

    @Value("${MOMO_REDIRECT_URL}")
    private String redirectUrl;

    @Value("${MOMO_IPN_URL}")
    private String ipnUrl;

    @Value("${MOMO_REQUEST_TYPE}")
    private String requestType;


    // tạo request QR tới momo
    @Transactional
    public MoMoPaymentResponse createPayment(CreatePaymentRequest request, User user) {
        try {
            if (user == null) {
                throw new RuntimeException("User authentication required");
            }
            String orderId = "POWER_GYM_" + System.currentTimeMillis();
            String requestId = "REQ_" + System.currentTimeMillis();
            String extraData = "";
            if (request.getExtraData() != null && !request.getExtraData().isEmpty()) {
                extraData = MoMoUtils.encodeBase64(request.getExtraData());
            }
            
            // Generate signature
            String signature = MoMoUtils.generatePaymentSignature(
                    accessKey,
                    request.getAmount(),
                    extraData,
                    ipnUrl,
                    orderId,
                    request.getOrderInfo(),
                    partnerCode,
                    redirectUrl,
                    requestId,
                    requestType,
                    secretKey
            );

            // Create MoMo payment request
            MoMoPaymentRequest momoRequest = MoMoPaymentRequest.builder()
                    .partnerCode(partnerCode)
                    .requestType(requestType)
                    .ipnUrl(ipnUrl)
                    .redirectUrl(redirectUrl)
                    .orderId(orderId)
                    .amount(request.getAmount())
                    .orderInfo(request.getOrderInfo())
                    .requestId(requestId)
                    .extraData(extraData)
                    .signature(signature)
                    .lang(request.getLang() != null ? request.getLang() : "vi")
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<MoMoPaymentRequest> entity = new HttpEntity<>(momoRequest, headers);

            ResponseEntity<MoMoPaymentResponse> response = restTemplate.postForEntity(
                endpoint, entity, MoMoPaymentResponse.class);

            MoMoPaymentResponse momoResponse = response.getBody();
            
            if (momoResponse != null && momoResponse.getResultCode() == 0) {
                PaymentOrder paymentOrder = new PaymentOrder();
                paymentOrder.setId(orderId);
                paymentOrder.setAmount(request.getAmount());
                paymentOrder.setContent(request.getOrderInfo());
                paymentOrder.setStatus(PaymentStatus.PENDING);
                paymentOrder.setUser(user);
                paymentOrder.setRequestId(requestId);
                paymentOrder.setExtraData(extraData);
                paymentOrder.setPaymentUrl(momoResponse.getPayUrl());
                paymentOrder.setQrCodeUrl(momoResponse.getQrCodeUrl());
                paymentOrder.setDeeplink(momoResponse.getDeeplink());
                paymentOrder.setPaymentMethod("MOMO");

                if (request.getItemType() != null) {
                    paymentOrder.setItemType(request.getItemType());
                    paymentOrder.setItemId(request.getItemId());
                    paymentOrder.setItemName(request.getItemName());
                }
                
                PaymentOrder savedOrder = paymentOrderRepository.save(paymentOrder);
                if (savedOrder.getUser() == null) {
                    throw new RuntimeException("Failed to save payment order with user");
                }
                
                return momoResponse;
            } else {
                log.error("MoMo payment creation failed: {}", momoResponse != null ? momoResponse.getMessage() : "Unknown error");
                throw new RuntimeException("Failed to create MoMo payment: " + 
                    (momoResponse != null ? momoResponse.getMessage() : "Unknown error"));
            }

        } catch (Exception e) {
            log.error("Error creating MoMo payment", e);
            throw new RuntimeException("Error creating MoMo payment: " + e.getMessage());
        }
    }


    // check PIN từ momo trả về
    @Transactional
    public void handleIPN(MoMoIPNRequest ipnRequest) {
        try {
            String expectedSignature = MoMoUtils.generateIPNSignature(
                accessKey,
                ipnRequest.getAmount(),
                ipnRequest.getExtraData(),
                ipnRequest.getMessage(),
                ipnRequest.getOrderId(),
                ipnRequest.getOrderInfo(),
                ipnRequest.getOrderType(),
                partnerCode,
                ipnRequest.getPayType(),
                ipnRequest.getRequestId(),
                ipnRequest.getResponseTime(),
                ipnRequest.getResultCode(),
                ipnRequest.getTransId(),
                secretKey
            );

            if (!expectedSignature.equals(ipnRequest.getSignature())) {
                log.error("Invalid MoMo IPN signature for order: {}", ipnRequest.getOrderId());
                throw new RuntimeException("Invalid signature");
            }
            PaymentOrder paymentOrder = paymentOrderRepository.findById(ipnRequest.getOrderId())
                .orElseThrow(() -> new RuntimeException("Payment order not found: " + ipnRequest.getOrderId()));

            if (ipnRequest.getResultCode() == 0) {
                paymentOrder.setStatus(PaymentStatus.SUCCESS);
                paymentOrder.setMomoTransId(ipnRequest.getTransId().toString());
                paymentOrder.setTransactionRef(ipnRequest.getTransId().toString());
                
                processSuccessfulPayment(paymentOrder);
            } else {
                paymentOrder.setStatus(PaymentStatus.FAILED);
            }

            paymentOrderRepository.save(paymentOrder);

        } catch (Exception e) {
            throw new RuntimeException("Error handling MoMo IPN: " + e.getMessage());
        }
    }

    // check status
    public PaymentOrder getPaymentStatus(String orderId) {
        PaymentOrder paymentOrder = paymentOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("Payment order not found: " + orderId));
        return paymentOrder;
    }

    /**
     * Cancel expired payments
     */
    @Transactional
    public void cancelExpiredPayments() {
        LocalDateTime now = LocalDateTime.now();
        paymentOrderRepository.findByStatusAndExpiredAtBefore(PaymentStatus.PENDING, now)
            .forEach(order -> {
                order.setStatus(PaymentStatus.EXPIRED);
                paymentOrderRepository.save(order);
                log.info("Expired payment order: {}", order.getId());
            });
    }

    /**
     * Get user payment history
     */
    @Transactional(readOnly = true)
    public List<PaymentOrder> getUserPaymentHistory(User user) {
        return paymentOrderRepository.findByUserOrderByCreatedAtDesc(user);
    }

    /**
     * Get payment status with trainer selection information
     */
    @Transactional(readOnly = true)
    public PaymentWithTrainerSelectionResponse getPaymentWithTrainerSelection(PaymentOrder paymentOrder) {
        log.info("Getting payment with trainer selection for order: {}", paymentOrder.getId());
        
        // Get user's service registrations that might need trainer selection
        List<ServiceRegistration> userRegistrations = serviceRegistrationRepository
            .findByUserAndStatusOrderByRegistrationDateDesc(
                paymentOrder.getUser(), ServiceRegistration.RegistrationStatus.ACTIVE);
        
        // Convert to trainer selection responses
        List<ServiceRegistrationWithTrainerSelectionResponse> registrationResponses = 
            userRegistrations.stream()
                .map(registration -> {
                    try {
                        return serviceRegistrationService.getRegistrationForTrainerSelection(registration.getId());
                    } catch (Exception e) {
                        log.warn("Could not get trainer selection for registration {}: {}", 
                            registration.getId(), e.getMessage());
                        return null;
                    }
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
        
        // Count registrations needing trainer selection
        int registrationsNeedingTrainer = (int) registrationResponses.stream()
            .filter(ServiceRegistrationWithTrainerSelectionResponse::needsTrainerSelection)
            .count();
        
        // Determine workflow status
        boolean paymentCompleted = paymentOrder.getStatus() == PaymentStatus.SUCCESS;
        boolean hasServiceRegistrations = !registrationResponses.isEmpty();
        boolean needsTrainerSelection = registrationsNeedingTrainer > 0;
        
        String nextAction;
        if (!paymentCompleted) {
            nextAction = "COMPLETE_PAYMENT";
        } else if (!hasServiceRegistrations) {
            nextAction = "REGISTER_SERVICE";
        } else if (needsTrainerSelection) {
            nextAction = "SELECT_TRAINER";
        } else {
            nextAction = "BOOK_SESSION";
        }
        
        return PaymentWithTrainerSelectionResponse.builder()
            .orderId(paymentOrder.getId())
            .transactionId(paymentOrder.getTransactionRef())
            .amount(paymentOrder.getAmount().doubleValue())
            .status(paymentOrder.getStatus().name())
            .paymentMethod(paymentOrder.getPaymentMethod())
            .createdAt(paymentOrder.getCreatedAt())
            .completedAt(paymentOrder.getCreatedAt()) // Use createdAt as completedAt for now
            .userId(paymentOrder.getUser().getId())
            .userFullName(paymentOrder.getUser().getFullName())
            .userEmail(paymentOrder.getUser().getEmail())
            .serviceRegistrations(registrationResponses)
            .totalRegistrations(registrationResponses.size())
            .registrationsNeedingTrainer(registrationsNeedingTrainer)
            .paymentCompleted(paymentCompleted)
            .hasServiceRegistrations(hasServiceRegistrations)
            .needsTrainerSelection(needsTrainerSelection)
            .nextAction(nextAction)
            .build();
    }

    /**
     * Process successful payment - register service or activate membership
     */
    private void processSuccessfulPayment(PaymentOrder paymentOrder) {
        try {
            if ("SERVICE".equals(paymentOrder.getItemType()) && paymentOrder.getItemId() != null) {
                    log.info("Service registration needed for user {} and service {}",
                    paymentOrder.getUser().getId(), paymentOrder.getItemId());
            } else if ("MEMBERSHIP".equals(paymentOrder.getItemType()) && paymentOrder.getItemId() != null) {
                // membershipService.activateMembershipForUser(paymentOrder.getUser(), paymentOrder.getItemId());
                log.info("Membership activation needed for user {} and package {}", 
                    paymentOrder.getUser().getId(), paymentOrder.getItemId());
            }
        } catch (Exception e) {
            log.error("Error processing successful payment for order: {}", paymentOrder.getId(), e);
        }
    }
}