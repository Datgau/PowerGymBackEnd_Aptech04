package com.example.project_backend04.dto.request;

import com.example.project_backend04.enums.DeliveryStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeliveryStatusRequest {
    @NotNull(message = "Delivery status is required")
    private DeliveryStatus deliveryStatus;
}
