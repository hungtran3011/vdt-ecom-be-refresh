package com.hungng3011.vdtecomberefresh.payment.dtos.viettel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViettelTransactionInitiationRequest {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("transAmount")
    private Long transAmount; // Amount in VND (smallest unit)
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("returnType")
    private String returnType; // WEB, QR, DEEPLINK
    
    @JsonProperty("returnUrl")
    private String returnUrl; // For WEB returnType
    
    @JsonProperty("cancelUrl")
    private String cancelUrl; // For WEB returnType
    
    @JsonProperty("paymentMethod")
    private String paymentMethod; // Optional
    
    @JsonProperty("expireAfter")
    private Integer expireAfter; // Minutes
    
    @JsonProperty("customerInfo")
    private CustomerInfo customerInfo; // Optional
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CustomerInfo {
        @JsonProperty("customerName")
        private String customerName;
        
        @JsonProperty("customerPhone")
        private String customerPhone;
        
        @JsonProperty("customerEmail")
        private String customerEmail;
        
        @JsonProperty("customerAddress")
        private String customerAddress;
    }
}
