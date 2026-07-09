package com.smartchunker.starter.config;

import com.smartchunker.starter.SmartChunkerTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ChunkerProperties.class)
@ConditionalOnProperty(prefix = "smart-chunker", name = "enable", havingValue = "true", matchIfMissing = true)
public class SmartChunkerAutoConfiguration {

    @Bean
    public SmartChunkerTemplate smartChunkerTemplate(ChunkerProperties properties) {
        return new SmartChunkerTemplate(properties);
    }
}