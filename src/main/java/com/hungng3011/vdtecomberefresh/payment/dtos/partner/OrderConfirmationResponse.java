package com.hungng3011.vdtecomberefresh.payment.dtos.partner;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderConfirmationResponse {
    
    @JsonProperty("code")
    private String code; // "00" for success, error codes for failures
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("message")
    private String message; // Optional error message
}
