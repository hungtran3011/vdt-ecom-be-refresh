package com.hungng3011.vdtecomberefresh.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.net.ssl.SSLContext;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

/**
 * Test Configuration to provide mock implementations of dependencies
 * that might be causing issues in tests.
 */
@TestConfiguration
@Profile("test")
public class TestConfig {

    /**
     * Provides a stub SSL Context for tests to avoid needing real SSL certificates
     */
    @Bean
    @ConditionalOnMissingBean(name = "sslContext")
    public SSLContext sslContext() throws Exception {
        // Create a default SSL context for testing
        return SSLContext.getDefault();
    }

    /**
     * Stub for any Apache TLS dependencies that might be needed
     */
    @Bean
    @ConditionalOnMissingBean(name = "tlsSocketStrategy")
    public Object tlsSocketStrategyStub() {
        // This is a stub bean just to satisfy dependency injection
        return new Object();
    }
}
