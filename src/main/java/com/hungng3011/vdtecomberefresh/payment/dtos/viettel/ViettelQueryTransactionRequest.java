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
public class ViettelQueryTransactionRequest {
    
    @JsonProperty("orderId")
    private String orderId; // For payment query
    
    @JsonProperty("originalRequestId")
    private String originalRequestId; // For refund query
}
