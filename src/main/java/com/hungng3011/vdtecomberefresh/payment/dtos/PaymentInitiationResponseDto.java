package com.hungng3011.vdtecomberefresh.payment.dtos;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiationResponseDto {
    private String paymentGatewayUrl; // For redirect-based flows
    private String clientSecret;      // For client-side SDKs (e.g., Stripe)
    private String transactionId;     // Gateway's transaction ID
    // Add other relevant fields
}