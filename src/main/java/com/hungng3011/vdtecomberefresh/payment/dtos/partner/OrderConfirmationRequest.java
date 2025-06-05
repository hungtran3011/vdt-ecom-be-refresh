package com.hungng3011.vdtecomberefresh.payment.dtos.partner;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderConfirmationRequest {
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("merchantCode")
    private String merchantCode;
    
    @JsonProperty("transAmount")
    private Long transAmount;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("paymentMethod")
    private String paymentMethod;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("vtRequestId")
    private String vtRequestId;
    
    @JsonProperty("customerInfo")
    private CustomerInfo customerInfo;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
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
