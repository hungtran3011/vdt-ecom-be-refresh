package com.hungng3011.vdtecomberefresh;

import com.hungng3011.vdtecomberefresh.payment.config.ViettelPaymentConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
@EnableConfigurationProperties(ViettelPaymentConfig.class)
public class VdtEcomBeRefreshApplication {

    public static void main(String[] args) {
        SpringApplication.run(VdtEcomBeRefreshApplication.class, args);
    }

}
