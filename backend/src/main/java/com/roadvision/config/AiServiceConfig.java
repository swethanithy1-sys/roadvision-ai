package com.roadvision.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AiServiceConfig {

    @Bean
    public RestClient aiServiceRestClient(
            @Value("${app.ai.service-url}") String serviceUrl,
            @Value("${app.ai.timeout-ms}") long timeoutMs
    ) {
        return RestClientFactory.buildClient(serviceUrl, timeoutMs);
    }

    @Bean
    public RestClient roboflowRestClient(
            @Value("${app.ai.roboflow.base-url}") String baseUrl,
            @Value("${app.ai.timeout-ms}") long timeoutMs
    ) {
        return RestClientFactory.buildClient(baseUrl, timeoutMs);
    }

    @Bean
    public RestClient geminiRestClient(
            @Value("${app.cost.gemini.base-url}") String baseUrl,
            @Value("${app.ai.timeout-ms}") long timeoutMs
    ) {
        return RestClientFactory.buildClient(baseUrl, timeoutMs);
    }
}
