package com.cinemabook.config;

import com.cinemabook.domain.streaming.StreamingProvider;
import com.cinemabook.infrastructure.streaming.LocalStreamingProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dependency Inversion: StreamingService depends on StreamingProvider (interface).
 * This config wires the concrete implementation.
 *
 * To use CloudFront/Widevine in prod: swap the bean here. StreamingService unchanged.
 */
@Configuration
public class StreamingConfig {

    @Bean
    public StreamingProvider streamingProvider(LocalStreamingProvider local) {
        return local;
    }
}