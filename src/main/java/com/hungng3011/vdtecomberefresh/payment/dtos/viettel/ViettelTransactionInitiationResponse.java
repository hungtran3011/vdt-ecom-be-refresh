package com.hungng3011.vdtecomberefresh.payment.dtos.viettel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViettelTransactionInitiationResponse {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private TransactionData data;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionData {
        @JsonProperty("url")
        private String url; // For WEB returnType
        
        @JsonProperty("qrCode")
        private String qrCode; // For QR returnType
        
        @JsonProperty("deepLink")
        private String deepLink; // For DEEPLINK returnType
        
        @JsonProperty("vtRequestId")
        private String vtRequestId; // Viettel's transaction ID
        
        @JsonProperty("orderId")
        private String orderId; // Partner's order ID
        
        @JsonProperty("transAmount")
        private Long transAmount;
    }
}
