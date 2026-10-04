package com.example.playlisttracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableMongoAuditing
public class AnonymousUserConfig {

    /**
     * RestTemplate for calling the YouTube Data API v3.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
