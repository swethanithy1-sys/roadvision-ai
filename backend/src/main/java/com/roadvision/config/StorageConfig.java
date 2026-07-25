package com.roadvision.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class StorageConfig {

    @Bean
    public RestClient supabaseRestClient(
            @Value("${app.storage.supabase.project-url}") String projectUrl,
            @Value("${app.storage.supabase.timeout-ms}") long timeoutMs
    ) {
        return RestClientFactory.buildClient(projectUrl + "/storage/v1", timeoutMs);
    }
}
