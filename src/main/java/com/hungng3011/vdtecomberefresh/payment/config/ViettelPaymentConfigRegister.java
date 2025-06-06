package com.hungng3011.vdtecomberefresh.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class that enables the ViettelPaymentConfig properties.
 * 
 * <p>This class is separate from the actual ViettelPaymentConfig class to avoid
 * duplicate bean creation. By using @EnableConfigurationProperties, we ensure that
 * Spring Boot creates a single bean from the properties binding process.</p>
 * 
 * <p>The separation of concerns follows the best practice for @ConfigurationProperties:
 * - ViettelPaymentConfig contains only the properties structure
 * - ViettelPaymentConfigRegister handles the registration of the bean</p>
 */
@Configuration
@EnableConfigurationProperties(ViettelPaymentConfig.class)
public class ViettelPaymentConfigRegister {
    // This class intentionally left empty as its only purpose is to register ViettelPaymentConfig
}
