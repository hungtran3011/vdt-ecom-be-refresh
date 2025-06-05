package com.hungng3011.vdtecomberefresh.payment.dtos.viettel;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViettelRefundResponse {
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("data")
    private RefundData data;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundData {
        @JsonProperty("transactionStatus")
        private Integer transactionStatus;
        
        @JsonProperty("orderId")
        private String orderId; // Partner's refund ID
        
        @JsonProperty("originalRequestId")
        private String originalRequestId; // Original vtRequestId
        
        @JsonProperty("transAmount")
        private Long transAmount;
        
        @JsonProperty("vtRequestId")
        private String vtRequestId; // Viettel's refund transaction ID
        
        @JsonProperty("errorCode")
        private String errorCode;
        
        @JsonProperty("errorMessage")
        private String errorMessage;
    }
}
