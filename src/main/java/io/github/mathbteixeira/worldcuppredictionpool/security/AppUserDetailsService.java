package io.github.mathbteixeira.worldcuppredictionpool.security;

import io.github.mathbteixeira.worldcuppredictionpool.user.persistence.UserAccountRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AppUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;

    public AppUserDetailsService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userAccountRepository.findByEmailIgnoreCase(username)
                .map(user -> User.withUsername(user.getEmail())
                        .password(user.getPasswordHash())
                        .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                        .disabled(!user.isEnabled())
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }
}
