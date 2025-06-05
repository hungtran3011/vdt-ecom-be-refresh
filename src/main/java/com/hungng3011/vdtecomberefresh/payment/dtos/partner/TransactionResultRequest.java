package com.hungng3011.vdtecomberefresh.payment.dtos.partner;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResultRequest {
    
    @JsonProperty("transactionStatus")
    private Integer transactionStatus;
    
    @JsonProperty("errorCode")
    private String errorCode;
    
    @JsonProperty("orderId")
    private String orderId;
    
    @JsonProperty("vtRequestId")
    private String vtRequestId;
    
    @JsonProperty("type")
    private String type; // PAYMENT or REFUND
    
    @JsonProperty("transAmount")
    private Long transAmount;
    
    @JsonProperty("currency")
    private String currency;
    
    @JsonProperty("paymentMethod")
    private String paymentMethod;
    
    @JsonProperty("paymentInstrument")
    private PaymentInstrument paymentInstrument;
    
    @JsonProperty("paymentDate")
    private String paymentDate;
    
    @JsonProperty("merchantCode")
    private String merchantCode;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("errorMessage")
    private String errorMessage;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PaymentInstrument {
        @JsonProperty("bankCode")
        private String bankCode;
        
        @JsonProperty("bankName")
        private String bankName;
        
        @JsonProperty("cardType")
        private String cardType;
        
        @JsonProperty("maskedPan")
        private String maskedPan;
    }
}
