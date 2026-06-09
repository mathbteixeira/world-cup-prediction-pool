package io.github.mathbteixeira.worldcuppredictionpool.config;

import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserRole;
import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile({"local", "dev"})
@ConditionalOnProperty(prefix = "app.demo.admin", name = "enabled", havingValue = "true")
public class LocalDemoUserSeeder implements ApplicationRunner {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String email;
    private final String password;

    public LocalDemoUserSeeder(UserAccountRepository userAccountRepository,
                               PasswordEncoder passwordEncoder,
                               @Value("${app.demo.admin.username:demo-admin}") String username,
                               @Value("${app.demo.admin.email:admin@example.com}") String email,
                               @Value("${app.demo.admin.password:admin12345}") String password) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userAccountRepository.existsByEmailIgnoreCase(email)
                || userAccountRepository.existsByUsernameIgnoreCase(username)) {
            return;
        }

        userAccountRepository.save(new UserAccount(
                username,
                email.toLowerCase(),
                passwordEncoder.encode(password),
                UserRole.ADMIN
        ));
    }
}
