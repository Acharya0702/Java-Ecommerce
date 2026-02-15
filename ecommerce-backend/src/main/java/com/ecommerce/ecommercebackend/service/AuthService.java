package com.ecommerce.ecommercebackend.service;

import com.ecommerce.ecommercebackend.dto.AuthRequest;
import com.ecommerce.ecommercebackend.dto.AuthResponse;
import com.ecommerce.ecommercebackend.dto.RegisterRequest;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.EmailAlreadyExistsException;
import com.ecommerce.ecommercebackend.exception.InvalidCredentialsException;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.exception.TokenExpiredException;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import com.ecommerce.ecommercebackend.security.CustomUserDetails;
import com.ecommerce.ecommercebackend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    public AuthResponse register(RegisterRequest request) {
        log.info("Starting registration for email: {}", request.getEmail());

        try {
            // Check if email already exists
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                log.warn("Email already exists: {}", request.getEmail());
                throw new EmailAlreadyExistsException("Email already registered");
            }

            log.debug("Creating user object for: {}", request.getEmail());
            // Create new user
            User user = new User();
            user.setEmail(request.getEmail());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setFirstName(request.getFirstName());
            user.setLastName(request.getLastName());
            user.setPhone(request.getPhone());
            user.setRole(User.Role.CUSTOMER);
            user.setIsActive(true);
            user.setIsEmailVerified(false);

            // Generate email verification token
            String verificationToken = UUID.randomUUID().toString();
            user.setEmailVerificationToken(verificationToken);

            // Initialize cart for the user
            user.initializeCart();

            log.debug("Saving user to database: {}", request.getEmail());
            User savedUser = userRepository.save(user);
            log.info("User saved with ID: {}", savedUser.getId());


            try {
                // Send verification email
                emailService.sendVerificationEmail(user.getEmail(), verificationToken);
                log.info("Verification email sent to: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Failed to send verification email to: {}", user.getEmail(), e);
                // Don't throw exception - user is created successfully, email can be resent later
            }


            // Convert User to UserDetails
            UserDetails userDetails = new CustomUserDetails(savedUser);

            // Generate tokens using UserDetails
            log.debug("Generating JWT tokens for user: {}", savedUser.getEmail());
            String jwtToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            log.info("Registration successful for: {}", savedUser.getEmail());

            return AuthResponse.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .user(mapToUserResponse(savedUser))
                    .build();

        } catch (Exception e) {
            log.error("Registration failed for email: {}", request.getEmail(), e);
            throw e; // Re-throw to see in logs
        }
    }

    public AuthResponse authenticate(AuthRequest request) {
        log.info("Authentication attempt for email: {}", request.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getEmail(),
                            request.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Check if email is verified
            if (!user.getIsEmailVerified()) {
                log.warn("Login attempt with unverified email: {}", request.getEmail());
                throw new InvalidCredentialsException("Please verify your email before logging in");
            }

            // Check if user is active
            if (!user.getIsActive()) {
                log.warn("Login attempt with inactive account: {}", request.getEmail());
                throw new InvalidCredentialsException("Account is deactivated. Please contact support");
            }

            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);

            // Convert User to UserDetails
            UserDetails userDetails = new CustomUserDetails(user);

            String jwtToken = jwtService.generateToken(userDetails);
            String refreshToken = jwtService.generateRefreshToken(userDetails);

            log.info("Authentication successful for: {}", user.getEmail());

            return AuthResponse.builder()
                    .accessToken(jwtToken)
                    .refreshToken(refreshToken)
                    .user(mapToUserResponse(user))
                    .build();

        } catch (Exception e) {
            log.error("Authentication failed for email: {}", request.getEmail(), e);
            throw new InvalidCredentialsException("Invalid email or password");
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        log.debug("Refreshing token");

        try {
            String email = jwtService.extractUsername(refreshToken);

            if (email == null) {
                log.warn("Invalid refresh token - cannot extract username");
                throw new InvalidCredentialsException("Invalid refresh token");
            }

            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Convert User to UserDetails for validation
            UserDetails userDetails = new CustomUserDetails(user);

            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                log.warn("Invalid refresh token for user: {}", email);
                throw new InvalidCredentialsException("Invalid refresh token");
            }

            String newAccessToken = jwtService.generateToken(userDetails);
            String newRefreshToken = jwtService.generateRefreshToken(userDetails);

            log.info("Token refreshed successfully for user: {}", email);

            return AuthResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .user(mapToUserResponse(user))
                    .build();

        } catch (Exception e) {
            log.error("Token refresh failed", e);
            throw new InvalidCredentialsException("Failed to refresh token");
        }
    }

    public void verifyEmail(String token) {
        log.info("Verifying email with token");

        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid verification token"));

        if (user.getIsEmailVerified()) {
            throw new IllegalStateException("Email already verified");
        }

        user.setIsEmailVerified(true);
        user.setEmailVerificationToken(null);
        userRepository.save(user);

        log.info("Email verified for user: {}", user.getEmail());

        try {
            // Send welcome email after verification
            emailService.sendWelcomeEmail(user.getEmail(), user.getFirstName() + " " + user.getLastName());
            log.info("Welcome email sent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send welcome email to: {}", user.getEmail(), e);
        }
    }

    public void resendVerificationEmail(String email) {
        log.info("Resending verification email to: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (user.getIsEmailVerified()) {
            throw new IllegalStateException("Email already verified");
        }

        // Generate new verification token
        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        userRepository.save(user);

        try {
            emailService.sendVerificationEmail(user.getEmail(), verificationToken);
            log.info("Verification email resent to: {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to resend verification email to: {}", user.getEmail(), e);
            throw new RuntimeException("Failed to send verification email");
        }
    }
    public void initiatePasswordReset(String email) {
        log.info("Initiating password reset for: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String resetToken = UUID.randomUUID().toString();
        user.setPasswordResetToken(resetToken);
        user.setPasswordResetExpiry(LocalDateTime.now().plusHours(24));
        userRepository.save(user);

        try {
            emailService.sendPasswordResetEmail(email, resetToken);
            log.info("Password reset email sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
            throw new RuntimeException("Failed to send password reset email");
        }
    }

    public void resetPassword(String token, String newPassword) {
        log.info("Resetting password with token");

        User user = userRepository.findByPasswordResetToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid reset token"));

        if (user.getPasswordResetExpiry() == null ||
                user.getPasswordResetExpiry().isBefore(LocalDateTime.now())) {
            throw new TokenExpiredException("Password reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpiry(null);
        userRepository.save(user);

        log.info("Password reset successfully for user: {}", user.getEmail());
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new InvalidCredentialsException("User not authenticated");
        }
        String email = authentication.getName();
        return getUserByEmail(email);
    }

    private AuthResponse.UserResponse mapToUserResponse(User user) {
        return AuthResponse.UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phone(user.getPhone())
                .role(user.getRole())
                .isEmailVerified(user.getIsEmailVerified())
                .profileImageUrl(user.getProfileImageUrl())
                .createdAt(user.getCreatedAt())
                .build();
    }
}