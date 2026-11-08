package com.canteen.config;

import com.canteen.entity.Role;
import com.canteen.entity.User;
import com.canteen.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** Public sign-up only creates students, so the first admin comes from the environment. */
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    @Value("${app.admin.email:}")
    private String email;

    @Value("${app.admin.password:}")
    private String password;

    @Override
    public void run(String... args) {
        if (email.isBlank() || password.isBlank() || userRepo.countByRole(Role.ADMIN) > 0) {
            return;
        }
        userRepo.save(User.builder()
                .name("Administrator")
                .email(email.trim().toLowerCase())
                .password(encoder.encode(password))
                .role(Role.ADMIN)
                .enabled(true)
                .build());
        log.info("Created the initial admin account");
    }
}
