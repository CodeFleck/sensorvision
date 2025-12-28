package io.indcloud.config;

import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.Organization;
import io.indcloud.model.Role;
import io.indcloud.model.User;
import io.indcloud.repository.OrganizationRepository;
import io.indcloud.repository.RoleRepository;
import io.indcloud.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * Initializes the default admin user on application startup.
 *
 * This component creates an admin user if:
 * 1. ADMIN_EMAIL and ADMIN_PASSWORD environment variables are set
 * 2. No user with the specified email exists
 *
 * SECURITY NOTES:
 * - Set ADMIN_EMAIL and ADMIN_PASSWORD only during initial deployment
 * - Remove/unset these environment variables after the admin user is created
 * - The password is hashed using BCrypt before storage
 * - This initializer will NOT recreate the user if it already exists
 *
 * Usage:
 * - Set environment variables: ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD=SecurePassword123
 * - Deploy/restart the application
 * - Verify admin user creation in logs
 * - Remove the environment variables and redeploy
 */
@Slf4j
@Component
public class AdminUserInitializer implements CommandLineRunner {

    @Value("${ADMIN_EMAIL:#{null}}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD:#{null}}")
    private String adminPassword;

    @Value("${ADMIN_FIRST_NAME:Admin}")
    private String adminFirstName;

    @Value("${ADMIN_LAST_NAME:User}")
    private String adminLastName;

    @Value("${ADMIN_ORG_NAME:System Administration}")
    private String adminOrgName;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // Only proceed if both email and password are provided
        if (adminEmail == null || adminPassword == null) {
            log.info("Admin user initialization skipped: ADMIN_EMAIL or ADMIN_PASSWORD not set");
            return;
        }

        // Check if user already exists
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Admin user initialization skipped: User with email '{}' already exists", adminEmail);
            return;
        }

        try {
            // Create admin user
            createAdminUser();
            log.info("=================================================================");
            log.info("ADMIN USER CREATED SUCCESSFULLY");
            log.info("Email: {}", adminEmail);
            log.info("=================================================================");
            log.warn("SECURITY REMINDER: Remove ADMIN_EMAIL and ADMIN_PASSWORD environment variables and redeploy");
            log.info("=================================================================");
        } catch (Exception e) {
            log.error("Failed to create admin user", e);
            throw new RuntimeException("Admin user initialization failed", e);
        }
    }

    private void createAdminUser() {
        // Find or create admin organization
        Organization organization = organizationRepository.findByName(adminOrgName)
                .orElseGet(() -> {
                    Organization org = Organization.builder()
                            .name(adminOrgName)
                            .build();
                    return organizationRepository.save(org);
                });

        // Get ADMIN role
        Role adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseThrow(() -> new RuntimeException("ROLE_ADMIN not found in database. Ensure migrations have run."));

        // Get USER role (admins should have both roles)
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER not found in database. Ensure migrations have run."));

        // Create username from email (before @ symbol)
        String username = adminEmail.split("@")[0];

        // Make username unique if it already exists
        String finalUsername = username;
        int counter = 1;
        while (userRepository.existsByUsername(finalUsername)) {
            finalUsername = username + counter;
            counter++;
        }

        // Hash the password
        String passwordHash = passwordEncoder.encode(adminPassword);

        // Create admin user
        User adminUser = User.builder()
                .username(finalUsername)
                .email(adminEmail)
                .passwordHash(passwordHash)
                .firstName(adminFirstName)
                .lastName(adminLastName)
                .organization(organization)
                .enabled(true)
                .emailVerified(true) // Auto-verify admin email
                .roles(new HashSet<>(Set.of(adminRole, userRole)))
                .build();

        userRepository.save(adminUser);

        log.info("Admin user created with username: {}", finalUsername);
        log.info("Organization: {}", organization.getName());
        log.info("Roles: ROLE_ADMIN, ROLE_USER");
    }
}
