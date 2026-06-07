package io.github.mathbteixeira.worldcuppredictionpool.config;

import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserRole;
import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LocalDemoUserSeederTest {

    private final UserAccountRepository userAccountRepository = mock(UserAccountRepository.class);
    private final PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

    @Test
    void shouldCreateLocalAdminWhenMissing() {
        when(passwordEncoder.encode("admin12345")).thenReturn("encoded-password");

        new LocalDemoUserSeeder(
                userAccountRepository,
                passwordEncoder,
                "demo-admin",
                "Admin@Example.com",
                "admin12345"
        ).run(null);

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(userCaptor.capture());
        UserAccount user = userCaptor.getValue();
        assertThat(user.getUsername()).isEqualTo("demo-admin");
        assertThat(user.getEmail()).isEqualTo("admin@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void shouldNotOverwriteExistingLocalAdmin() {
        when(userAccountRepository.existsByEmailIgnoreCase("admin@example.com")).thenReturn(true);

        new LocalDemoUserSeeder(
                userAccountRepository,
                passwordEncoder,
                "demo-admin",
                "admin@example.com",
                "admin12345"
        ).run(null);

        verify(userAccountRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(passwordEncoder, never()).encode(org.mockito.ArgumentMatchers.anyString());
    }
}
