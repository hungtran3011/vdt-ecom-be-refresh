package com.hungng3011.vdtecomberefresh.media;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.mockito.Mockito;

/**
 * Test configuration for MediaControllerTest
 * Provides mock beans needed for the controller tests
 */
@TestConfiguration
public class MediaControllerTestConfig {
    
    @Bean
    @Primary
    public MediaService mediaService() {
        return Mockito.mock(MediaService.class);
    }
}
