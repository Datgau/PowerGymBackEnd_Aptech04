package com.example.project_backend04.mapper;

import com.example.project_backend04.dto.response.Payment.PaymentOrderResponse;
import com.example.project_backend04.dto.response.User.UserShortResponse;
import com.example.project_backend04.entity.PaymentOrder;
import com.example.project_backend04.entity.User;
import org.springframework.stereotype.Component;

@Component
public class PaymentOrderMapper {

    public PaymentOrderResponse toResponse(PaymentOrder paymentOrder) {
        if (paymentOrder == null) {
            return null;
        }

        return PaymentOrderResponse.builder()
                .id(paymentOrder.getId())
                .amount(paymentOrder.getAmount())
                .content(paymentOrder.getContent())
                .status(paymentOrder.getStatus())
                .createdAt(paymentOrder.getCreatedAt())
                .expiredAt(paymentOrder.getExpiredAt())
                .retryCount(paymentOrder.getRetryCount())
                .transactionRef(paymentOrder.getTransactionRef())
                .momoTransId(paymentOrder.getMomoTransId())
                .paymentMethod(paymentOrder.getPaymentMethod())
                .paymentUrl(paymentOrder.getPaymentUrl())
                .qrCodeUrl(paymentOrder.getQrCodeUrl())
                .deeplink(paymentOrder.getDeeplink())
                .requestId(paymentOrder.getRequestId())
                .extraData(paymentOrder.getExtraData())
                .itemType(paymentOrder.getItemType())
                .itemId(paymentOrder.getItemId())
                .itemName(paymentOrder.getItemName())
                .user(mapUserToShortResponse(paymentOrder.getUser()))
                .build();
    }

    private UserShortResponse mapUserToShortResponse(User user) {
        if (user == null) {
            return null;
        }

        return UserShortResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .username(user.getFullName().replaceAll("\\s+", "").toLowerCase())
                .build();
    }
}