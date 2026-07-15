package com.code.back_end.seeder;

import com.code.back_end.entity.User;
import com.code.back_end.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log =
            LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository repo;
    private final PasswordEncoder encoder;
    private final String adminUsername;
    private final String adminPassword;

    public AdminSeeder(
            UserRepository repo,
            PasswordEncoder encoder,
            @Value("${app.admin.username:admin}")
            String adminUsername,
            @Value("${app.admin.password:}")
            String adminPassword
    ) {
        this.repo = repo;
        this.encoder = encoder;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (adminPassword == null || adminPassword.isBlank()) {
            log.info(
                    "Default admin seeding skipped because ADMIN_PASSWORD is not set."
            );
            return;
        }

        if (repo.findByUsername(adminUsername).isEmpty()) {

            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(encoder.encode(adminPassword));
            admin.setRole("ADMIN");

            repo.save(admin);
        }
    }
}
