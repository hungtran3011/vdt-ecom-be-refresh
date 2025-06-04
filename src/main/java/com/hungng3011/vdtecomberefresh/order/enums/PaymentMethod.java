package com.hungng3011.vdtecomberefresh.order.enums;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum PaymentMethod {
    CASH_ON_DELIVERY("Cash on Delivery"),
    CREDIT_CARD("Credit Card"),
    VIETTEL_MONEY("Viettel Money");

    private final String displayName;
}
