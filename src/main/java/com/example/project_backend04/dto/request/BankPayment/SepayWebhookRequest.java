package com.example.project_backend04.dto.request.BankPayment;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SepayWebhookRequest {
    
    @JsonProperty("content")
    @NotBlank(message = "Content is required")
    @Size(max = 500, message = "Content must not exceed 500 characters")

    private String description;
    
    @JsonProperty("transferAmount")
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private Integer amount;
    
    @JsonProperty("referenceCode")
    private String transactionId;
    
    @JsonProperty("transactionDate")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;


    @JsonProperty("transferType")
    @NotBlank(message = "Transfer type is required")
    private String transferType; // "in" hoặc "out"
}
