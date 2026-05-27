package io.github.mathbteixeira.worldcuppredictionpool.user.persistence;

import io.github.mathbteixeira.worldcuppredictionpool.user.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByUsernameIgnoreCase(String username);

    Optional<UserAccount> findByEmailIgnoreCase(String email);
}
