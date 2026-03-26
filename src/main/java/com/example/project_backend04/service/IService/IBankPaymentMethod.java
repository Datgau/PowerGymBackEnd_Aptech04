package com.example.project_backend04.service.IService;

import com.example.project_backend04.dto.request.BankPayment.SepayWebhookRequest;

import java.util.Map;

public interface IBankPaymentMethod {
    Map<String, Object> createBankPayment(Long userId, Long serviceId);
    void handleSepayWebhook(SepayWebhookRequest payload);
    String checkPaymentStatus(String content);

}
