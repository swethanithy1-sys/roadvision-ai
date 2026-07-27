package com.roadvision.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class SupabaseAuthConfig {

    @Bean
    public RestClient supabaseAuthRestClient(
            @Value("${app.supabase.project-url}") String projectUrl
    ) {
        return RestClientFactory.buildClient(projectUrl + "/auth/v1", 15000);
    }
}
