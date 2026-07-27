package com.roadvision.config;

import com.roadvision.common.constants.Role;
import com.roadvision.user.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.UUID;

/**
 * Seeds demo accounts on first startup so dashboards have data without manual signup. Accounts
 * are created through Supabase Auth's Admin API (with email_confirm=true so demo logins work
 * immediately) — the auth.users insert trigger (V3 migration) creates the matching profile row
 * automatically, defaulting to CITIZEN; the admin account's profile is then promoted.
 */
@Component
@Order(1)
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "Password123!";

    private final UserRepository userRepository;
    private final RestClient authClient;
    private final String secretKey;
    private final boolean seedEnabled;

    public DataSeeder(
            UserRepository userRepository,
            RestClient supabaseAuthRestClient,
            @Value("${app.supabase.secret-key}") String secretKey,
            @Value("${app.seed.enabled:true}") boolean seedEnabled
    ) {
        this.userRepository = userRepository;
        this.authClient = supabaseAuthRestClient;
        this.secretKey = secretKey;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(String... args) {
        if (!seedEnabled || userRepository.count() > 0) {
            return;
        }

        UUID adminId = createSupabaseUser(authClient, "admin@roadvision.ai", "Admin User", "9000000001");
        userRepository.findById(adminId).ifPresent(user -> {
            user.setRole(Role.ADMIN);
            userRepository.save(user);
        });

        createSupabaseUser(authClient, "citizen1@roadvision.ai", "Asha Rao", "9000000002");
        createSupabaseUser(authClient, "citizen2@roadvision.ai", "Vikram Singh", "9000000003");

        log.info("Seeded demo accounts via Supabase Auth (admin@roadvision.ai / citizen1@roadvision.ai / citizen2@roadvision.ai), password: {}", DEMO_PASSWORD);
    }

    @SuppressWarnings("unchecked")
    private UUID createSupabaseUser(RestClient authClient, String email, String fullName, String phone) {
        Map<String, Object> body = Map.of(
                "email", email,
                "password", DEMO_PASSWORD,
                "email_confirm", true,
                "user_metadata", Map.of("full_name", fullName, "phone", phone)
        );

        Map<String, Object> response = authClient.post()
                .uri("/admin/users")
                .header("apikey", secretKey)
                .header("Authorization", "Bearer " + secretKey)
                .body(body)
                .retrieve()
                .body(Map.class);

        return UUID.fromString((String) response.get("id"));
    }
}
