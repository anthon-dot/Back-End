package com.code.back_end.seeder;

import com.code.back_end.entity.User;
import com.code.back_end.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public AdminSeeder(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (repo.findByUsername("admin").isEmpty()) {

            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(encoder.encode("admin123"));
            admin.setRole("ADMIN");

            repo.save(admin);
        }
    }
}