package com.roadvision.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class EmailConfig {

    @Bean
    public RestClient resendRestClient(
            @Value("${app.email.resend.base-url}") String baseUrl,
            @Value("${app.email.resend.timeout-ms}") long timeoutMs
    ) {
        return RestClientFactory.buildClient(baseUrl, timeoutMs);
    }
}
