package com.hungng3011.vdtecomberefresh.order.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Represents payment methods available in the system.
 * Each method includes display names in English and Vietnamese.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentMethod {
    CASH_ON_DELIVERY("COD", "Cash on Delivery", "Thanh toán khi nhận hàng"),
    CREDIT_CARD("CARD", "Credit Card", "Thẻ tín dụng"),
    VIETTEL_MONEY("VIETTEL", "Viettel Money", "Viettel Money");
    
    private final String code;            // Code for API communication
    private final String displayName;      // Display name (English)
    private final String displayNameVi;    // Display name (Vietnamese)
    
    /**
     * Get the display name based on the current locale
     */
    public String getDisplayName(boolean isVietnamese) {
        return isVietnamese ? displayNameVi : displayName;
    }
}
