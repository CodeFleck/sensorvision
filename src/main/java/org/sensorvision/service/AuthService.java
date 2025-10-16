package org.sensorvision.service;

import org.sensorvision.dto.JwtAuthenticationResponse;
import org.sensorvision.dto.LoginRequest;
import org.sensorvision.dto.RegisterRequest;
import org.sensorvision.dto.UserResponse;
import org.sensorvision.exception.BadRequestException;
import org.sensorvision.model.Organization;
import org.sensorvision.model.Role;
import org.sensorvision.model.User;
import org.sensorvision.repository.OrganizationRepository;
import org.sensorvision.repository.RoleRepository;
import org.sensorvision.repository.UserRepository;
import org.sensorvision.security.CustomUserDetailsService;
import org.sensorvision.security.JwtService;
import org.sensorvision.security.UserPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private EmailNotificationService emailNotificationService;

    @Transactional
    public JwtAuthenticationResponse login(LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UserDetails userDetails = userDetailsService.loadUserById(userPrincipal.getId());

        String accessToken = jwtService.generateAccessToken(userDetails, userPrincipal.getId(), userPrincipal.getOrganizationId());

        // Use extended refresh token if "Remember Me" is enabled
        String refreshToken;
        if (loginRequest.isRememberMe()) {
            refreshToken = jwtService.generateRememberMeRefreshToken(userDetails, userPrincipal.getId(), userPrincipal.getOrganizationId());
        } else {
            refreshToken = jwtService.generateRefreshToken(userDetails, userPrincipal.getId(), userPrincipal.getOrganizationId());
        }

        return new JwtAuthenticationResponse(
                accessToken,
                refreshToken,
                userPrincipal.getId(),
                userPrincipal.getUsername(),
                userPrincipal.getEmail(),
                userPrincipal.getOrganizationId()
        );
    }

    @Transactional
    public JwtAuthenticationResponse register(RegisterRequest registerRequest) {
        // Check if username already exists
        if (userRepository.existsByUsername(registerRequest.getUsername())) {
            throw new BadRequestException("Username is already taken");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new BadRequestException("Email is already in use");
        }

        // Get or create organization
        Organization organization;
        if (registerRequest.getOrganizationName() != null && !registerRequest.getOrganizationName().isEmpty()) {
            organization = organizationRepository.findByName(registerRequest.getOrganizationName())
                    .orElseGet(() -> {
                        Organization newOrg = Organization.builder()
                                .name(registerRequest.getOrganizationName())
                                .build();
                        return organizationRepository.save(newOrg);
                    });
        } else {
            // Use default organization
            organization = organizationRepository.findByName("Default Organization")
                    .orElseThrow(() -> new RuntimeException("Default organization not found"));
        }

        // Get default user role
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("User Role not found"));

        // Create user
        User user = User.builder()
                .username(registerRequest.getUsername())
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .firstName(registerRequest.getFirstName())
                .lastName(registerRequest.getLastName())
                .organization(organization)
                .enabled(true)
                .roles(new HashSet<>(Set.of(userRole)))
                .build();

        User savedUser = userRepository.save(user);

        // Auto-login after registration
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registerRequest.getUsername(),
                        registerRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        UserDetails userDetails = userDetailsService.loadUserById(userPrincipal.getId());

        String accessToken = jwtService.generateAccessToken(userDetails, savedUser.getId(), savedUser.getOrganization().getId());
        String refreshToken = jwtService.generateRefreshToken(userDetails, savedUser.getId(), savedUser.getOrganization().getId());

        return new JwtAuthenticationResponse(
                accessToken,
                refreshToken,
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getOrganization().getId()
        );
    }

    public UserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        User user;

        // Handle OAuth2 JWT authentication (for requests with JWT token)
        if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String username = jwt.getSubject(); // The 'sub' claim contains the username

            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

            // Eagerly load associations
            user = userRepository.findByIdWithOrganizationAndRoles(user.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        // Handle UserPrincipal authentication (for login/register responses)
        else if (authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            user = userRepository.findByIdWithOrganizationAndRoles(userPrincipal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        else {
            throw new RuntimeException("Invalid authentication principal type: " +
                authentication.getPrincipal().getClass().getName());
        }

        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .organizationId(user.getOrganization().getId())
                .organizationName(user.getOrganization().getName())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .enabled(user.getEnabled())
                .build();
    }

    @Transactional
    public JwtAuthenticationResponse refreshToken(String refreshToken) {
        // Validate that it's a refresh token
        if (!jwtService.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        if (!jwtService.isRefreshToken(refreshToken)) {
            throw new BadRequestException("Token is not a refresh token");
        }

        // Extract username from refresh token
        String username = jwtService.getUsernameFromToken(refreshToken);

        // Load user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        // Load user details with associations
        user = userRepository.findByIdWithOrganizationAndRoles(user.getId())
                .orElseThrow(() -> new BadRequestException("User not found"));

        UserDetails userDetails = userDetailsService.loadUserById(user.getId());

        // Generate new tokens
        String newAccessToken = jwtService.generateAccessToken(userDetails, user.getId(), user.getOrganization().getId());
        String newRefreshToken = jwtService.generateRefreshToken(userDetails, user.getId(), user.getOrganization().getId());

        return new JwtAuthenticationResponse(
                newAccessToken,
                newRefreshToken,
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getOrganization().getId()
        );
    }

    @Transactional
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found with email: " + email));

        // Generate password reset token
        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1)); // Token valid for 1 hour

        userRepository.save(user);

        // Send email with reset link
        emailNotificationService.sendPasswordResetEmail(user.getEmail(), resetToken);
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid password reset token"));

        // Check if token has expired
        if (user.getPasswordResetTokenExpiry() == null ||
            user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Password reset token has expired");
        }

        // Update password
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        // Clear reset token
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);

        userRepository.save(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid email verification token"));

        // Check if token has expired
        if (user.getEmailVerificationTokenExpiry() == null ||
            user.getEmailVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Email verification token has expired");
        }

        // Mark email as verified
        user.setEmailVerified(true);

        // Clear verification token
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);

        userRepository.save(user);
    }

    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found with email: " + email));

        if (user.getEmailVerified()) {
            throw new BadRequestException("Email already verified");
        }

        // Generate new verification token
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(24)); // Token valid for 24 hours

        userRepository.save(user);

        // Send verification email
        emailNotificationService.sendVerificationEmail(user.getEmail(), verificationToken);
    }
}
