package com.ecommerce.ecommercebackend.dto;

import com.ecommerce.ecommercebackend.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    // Basic user information
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String fullName;
    private String phone;

    // Address information
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String country;

    // Account information
    private User.Role role;
    private Boolean isActive;
    private Boolean isEmailVerified;

    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;

    // Profile
    private String profileImageUrl;
    private String preferredLanguage;
    private String currency;
    private Boolean newsletterSubscribed;

    // Additional fields for admin view
    private Integer totalOrders;
    private Double totalSpent;
    private String lastLoginFormatted;
    private String createdAtFormatted;
    private String roleDisplay;
    private String statusDisplay;
    private String statusBadgeColor;

    // Constructor from User entity
    public static UserDTO fromEntity(User user) {
        if (user == null) return null;

        UserDTO dto = new UserDTO();

        // Basic info
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());

        // Address
        dto.setAddress(user.getAddress());
        dto.setCity(user.getCity());
        dto.setState(user.getState());
        dto.setZipCode(user.getZipCode());
        dto.setCountry(user.getCountry());

        // Account info
        dto.setRole(user.getRole());
        dto.setIsActive(user.getIsActive());
        dto.setIsEmailVerified(user.getIsEmailVerified());

        // Timestamps
        dto.setCreatedAt(user.getCreatedAt());
        dto.setUpdatedAt(user.getUpdatedAt());
        dto.setLastLogin(user.getLastLogin());

        // Profile
        dto.setProfileImageUrl(user.getProfileImageUrl());
        dto.setPreferredLanguage(user.getPreferredLanguage());
        dto.setCurrency(user.getCurrency());
        dto.setNewsletterSubscribed(user.getNewsletterSubscribed());

        // Format dates for display
        if (user.getCreatedAt() != null) {
            dto.setCreatedAtFormatted(user.getCreatedAt()
                    .format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a")));
        }

        if (user.getLastLogin() != null) {
            dto.setLastLoginFormatted(user.getLastLogin()
                    .format(DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' hh:mm a")));
        }

        // Format role for display
        if (user.getRole() != null) {
            String roleStr = user.getRole().toString();
            dto.setRoleDisplay(roleStr.substring(0, 1) + roleStr.substring(1).toLowerCase());
        }

        // Format status for display
        if (user.getIsActive() != null) {
            dto.setStatusDisplay(user.getIsActive() ? "Active" : "Inactive");
            dto.setStatusBadgeColor(user.getIsActive() ? "green" : "gray");
        }

        return dto;
    }

    // Helper method to update entity from DTO
    public User updateEntity(User user) {
        if (this.firstName != null) user.setFirstName(this.firstName);
        if (this.lastName != null) user.setLastName(this.lastName);
        if (this.phone != null) user.setPhone(this.phone);
        if (this.address != null) user.setAddress(this.address);
        if (this.city != null) user.setCity(this.city);
        if (this.state != null) user.setState(this.state);
        if (this.zipCode != null) user.setZipCode(this.zipCode);
        if (this.country != null) user.setCountry(this.country);
        if (this.preferredLanguage != null) user.setPreferredLanguage(this.preferredLanguage);
        if (this.currency != null) user.setCurrency(this.currency);
        if (this.newsletterSubscribed != null) user.setNewsletterSubscribed(this.newsletterSubscribed);
        if (this.profileImageUrl != null) user.setProfileImageUrl(this.profileImageUrl);

        return user;
    }
}