package org.sensorvision.security;

import org.sensorvision.model.Organization;
import org.sensorvision.model.Role;
import org.sensorvision.model.User;
import org.sensorvision.repository.OrganizationRepository;
import org.sensorvision.repository.RoleRepository;
import org.sensorvision.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");

        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        // Find or create user
        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createNewOAuth2User(email, name, registrationId));

        // Return OAuth2User with user details
        return new CustomOAuth2User(oauth2User, user);
    }

    private User createNewOAuth2User(String email, String name, String provider) {
        // Create a unique organization for this user to ensure data isolation
        String orgName = email.split("@")[0] + "'s Organization";
        Organization organization = Organization.builder()
                .name(orgName)
                .build();
        organization = organizationRepository.save(organization);

        // Get user role
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("User Role not found"));

        // Extract first and last name
        String[] nameParts = name != null ? name.split(" ", 2) : new String[]{"", ""};
        String firstName = nameParts.length > 0 ? nameParts[0] : "";
        String lastName = nameParts.length > 1 ? nameParts[1] : "";

        // Create username from email
        String username = email.split("@")[0] + "_" + provider;

        // Check if username exists and make it unique
        String finalUsername = username;
        int counter = 1;
        while (userRepository.existsByUsername(finalUsername)) {
            finalUsername = username + counter;
            counter++;
        }

        // Create new user
        User user = User.builder()
                .username(finalUsername)
                .email(email)
                .passwordHash(UUID.randomUUID().toString()) // Random password for OAuth2 users
                .firstName(firstName)
                .lastName(lastName)
                .organization(organization)
                .enabled(true)
                .emailVerified(true) // OAuth2 providers verify email
                .roles(new HashSet<>(Set.of(userRole)))
                .build();

        return userRepository.save(user);
    }
}
