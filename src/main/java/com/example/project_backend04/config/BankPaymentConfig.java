package com.example.project_backend04.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;



@Validated
@ConfigurationProperties(prefix = "bank.payment")
public record BankPaymentConfig(

        @NotBlank(message = "Bank code must not be empty (e.g., MB)")
        String bankCode,

        @NotBlank(message = "Account number must be configured")
        String accountNo,

        @NotBlank(message = "Account name must be configured")
        String accountName,

        @NotBlank(message = "SePay API Key is mandatory for webhook verification")
        String sepayApiKey,

        @NotBlank(message = "VietQR base URL must not be blank")
        String vietqrBaseUrl,

        @Min(value = 1, message = "Expiry minutes must be at least 1")
        Integer paymentExpiryMinutes,

        @NotNull(message = "Webhook configuration must not be null")
        WebhookConfig webhook
) {
    public record WebhookConfig(
            @Min(value = 1, message = "Rate limit per minute must be at least 1")
            Integer rateLimitPerMinute,

            @NotBlank(message = "API key header must not be empty")
            String apiKeyHeader,

            @NotBlank(message = "Webhook path must not be empty")
            String path
    ) {}
}