package io.indcloud.security;

import io.indcloud.model.User;
import io.indcloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        // Single query lookup to prevent timing attacks that could enumerate users
        // Uses case-insensitive matching for both username and email
        User user = userRepository.findByUsernameOrEmailIgnoreCase(usernameOrEmail)
                .orElseThrow(() ->
                        // Use generic error message to prevent information leakage
                        new UsernameNotFoundException("Invalid credentials")
                );

        // Block soft-deleted users from authenticating
        // Use same generic message to prevent account enumeration
        if (user.isDeleted()) {
            throw new UsernameNotFoundException("Invalid credentials");
        }

        return UserPrincipal.create(user);
    }

    @Transactional
    public UserDetails loadUserById(Long id) {
        User user = userRepository.findByIdWithOrganizationAndRoles(id)
                .orElseThrow(() ->
                        new UsernameNotFoundException("User not found with id: " + id)
                );

        // Block soft-deleted users from authenticating
        if (user.isDeleted()) {
            throw new UsernameNotFoundException("User account has been deleted with id: " + id);
        }

        return UserPrincipal.create(user);
    }
}
