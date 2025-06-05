package com.hungng3011.vdtecomberefresh.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Unified payment status enum used across the application.
 * Represents all possible states of a payment transaction.
 * Designed to map both to internal order states and external 
 * payment gateway (Viettel Money) statuses.
 */
@Getter
@RequiredArgsConstructor
public enum PaymentStatus {
    PENDING("00", "Pending", "Chờ thanh toán"),
    SUCCESSFUL("01", "Completed", "Thanh toán thành công"),
    FAILED("99", "Failed", "Thanh toán thất bại"),
    REFUNDED("02", "Refunded", "Đã hoàn tiền"),
    REFUND_PENDING("03", "Refund Pending", "Đang xử lý hoàn tiền");

    private final String code;           // Code for API communication
    private final String statusDisplay;   // Display name (English)
    private final String statusDisplayVi; // Display name (Vietnamese)
    
    /**
     * Map from Viettel Money transaction status to internal PaymentStatus
     * 
     * @param viettelStatus The status code from Viettel Money
     * @return The corresponding internal PaymentStatus
     */
    public static PaymentStatus fromViettelStatus(String viettelStatus) {
        return switch(viettelStatus) {
            case "00" -> PENDING;
            case "01" -> SUCCESSFUL;
            case "02" -> REFUNDED;
            case "03" -> REFUND_PENDING;
            default -> FAILED;
        };
    }
    
    /**
     * Get the string representation of the status for storage or display
     */
    @Override
    public String toString() {
        return statusDisplay;
    }
}
