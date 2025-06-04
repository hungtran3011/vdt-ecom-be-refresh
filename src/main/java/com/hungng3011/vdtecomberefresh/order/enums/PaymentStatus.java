package com.hungng3011.vdtecomberefresh.order.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum PaymentStatus {
    PENDING("Pending"),
    COMPLETED("Completed"),
    FAILED("Failed"),
    REFUNDED("Refunded");

    private final String status;
}
