package com.hungng3011.vdtecomberefresh.payment.utils;

import com.hungng3011.vdtecomberefresh.common.enums.PaymentStatus;

/**
 * Utility class to handle Viettel Money payment status conversion
 * and other payment-related helper functions.
 */
public class PaymentStatusUtils {

    /**
     * Maps Viettel Money payment status codes to internal PaymentStatus enum
     * 
     * @param transactionStatus Viettel Money transaction status code
     * @return Corresponding PaymentStatus enum value
     */
    public static PaymentStatus mapViettelTransactionStatus(String transactionStatus) {
        return PaymentStatus.fromViettelStatus(transactionStatus);
    }
    
    /**
     * Maps internal PaymentStatus enum to Viettel Money status code
     * 
     * @param status Internal PaymentStatus enum
     * @return Corresponding Viettel Money status code
     */
    public static String mapToViettelStatus(PaymentStatus status) {
        return status.getCode();
    }
    
    /**
     * Checks if a payment status indicates that a refund is allowed
     * 
     * @param status The payment status to check
     * @return true if refund is allowed for this status
     */
    public static boolean isRefundAllowed(PaymentStatus status) {
        return status == PaymentStatus.SUCCESSFUL;
    }
}
