package com.example.project_backend04.dto.request.Payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoMoPaymentRequest {
    private String partnerCode;
    private String requestType;
    private String ipnUrl;
    private String redirectUrl;
    private String orderId;
    private Long amount;
    private String orderInfo;
    private String requestId;
    private String extraData;
    private String signature;
    private String lang;
}