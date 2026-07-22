package com.roadvision.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class AiServiceConfig {

    @Bean
    public RestClient aiServiceRestClient(
            @Value("${app.ai.service-url}") String serviceUrl,
            @Value("${app.ai.timeout-ms}") long timeoutMs
    ) {
        return buildClient(serviceUrl, timeoutMs);
    }

    @Bean
    public RestClient roboflowRestClient(
            @Value("${app.ai.roboflow.base-url}") String baseUrl,
            @Value("${app.ai.timeout-ms}") long timeoutMs
    ) {
        return buildClient(baseUrl, timeoutMs);
    }

    private RestClient buildClient(String baseUrl, long timeoutMs) {
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(
                ClientHttpRequestFactorySettings.DEFAULTS
                        .withConnectTimeout(Duration.ofMillis(timeoutMs))
                        .withReadTimeout(Duration.ofMillis(timeoutMs))
        );

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
