package com.roadvision.config;

import com.roadvision.common.constants.Role;
import com.roadvision.user.User;
import com.roadvision.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds demo accounts on first startup so dashboards have data without manual signup.
 * Passwords are hashed through the real {@link PasswordEncoder} bean rather than
 * baked into SQL, keeping the seed portable across bcrypt versions.
 */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private static final String DEMO_PASSWORD = "Password123!";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) {
        if (!seedEnabled || userRepository.count() > 0) {
            return;
        }

        userRepository.save(verifiedDemoUser(
                "Admin User", "admin@roadvision.ai", "9000000001", Role.ADMIN
        ));
        userRepository.save(verifiedDemoUser(
                "Asha Rao", "citizen1@roadvision.ai", "9000000002", Role.CITIZEN
        ));
        userRepository.save(verifiedDemoUser(
                "Vikram Singh", "citizen2@roadvision.ai", "9000000003", Role.CITIZEN
        ));

        log.info("Seeded demo accounts (admin@roadvision.ai / citizen1@roadvision.ai / citizen2@roadvision.ai), password: {}", DEMO_PASSWORD);
    }

    private User verifiedDemoUser(String fullName, String email, String phone, Role role) {
        User user = new User(fullName, email, passwordEncoder.encode(DEMO_PASSWORD), phone, role);
        user.setEmailVerified(true);
        return user;
    }
}
