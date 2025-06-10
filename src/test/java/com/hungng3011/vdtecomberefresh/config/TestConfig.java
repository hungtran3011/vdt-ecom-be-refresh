package com.hungng3011.vdtecomberefresh.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import javax.net.ssl.SSLContext;

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
    public LayeredConnectionSocketFactory tlsSocketStrategyStub() {
        // Return a mock implementation or use Mockito to create a stub
        return org.mockito.Mockito.mock(LayeredConnectionSocketFactory.class);
    }
}
