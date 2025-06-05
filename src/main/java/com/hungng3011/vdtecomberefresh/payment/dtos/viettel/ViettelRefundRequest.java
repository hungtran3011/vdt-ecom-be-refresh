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
public class ViettelRefundRequest {
    
    @JsonProperty("transAmount")
    private Long transAmount; // Amount to refund in VND
    
    @JsonProperty("orderId")
    private String orderId; // Partner's refund ID
    
    @JsonProperty("originalRequestId")
    private String originalRequestId; // Viettel's vtRequestId of original payment
    
    @JsonProperty("description")
    private String description; // Optional refund description
}
