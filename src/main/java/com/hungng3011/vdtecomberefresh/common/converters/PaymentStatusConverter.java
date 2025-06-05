package com.hungng3011.vdtecomberefresh.common.converters;

import com.hungng3011.vdtecomberefresh.common.enums.PaymentStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * JPA converter for PaymentStatus enum to ensure consistent
 * database representation and easier querying.
 */
@Converter(autoApply = true)
public class PaymentStatusConverter implements AttributeConverter<PaymentStatus, String> {

    @Override
    public String convertToDatabaseColumn(PaymentStatus status) {
        return status == null ? null : status.getCode();
    }

    @Override
    public PaymentStatus convertToEntityAttribute(String code) {
        if (code == null) {
            return null;
        }
        
        for (PaymentStatus status : PaymentStatus.values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
}
