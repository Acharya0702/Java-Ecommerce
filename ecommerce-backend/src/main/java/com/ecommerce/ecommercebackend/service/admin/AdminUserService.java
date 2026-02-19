package com.ecommerce.ecommercebackend.service.admin;

import com.ecommerce.ecommercebackend.dto.UserDTO;
import com.ecommerce.ecommercebackend.entity.Order;
import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.exception.ResourceNotFoundException;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminUserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable, String role, String search) {
        User.Role userRole = null;
        if (role != null && !role.isEmpty()) {
            try {
                userRole = User.Role.valueOf(role.toUpperCase());
            } catch (IllegalArgumentException e) {
                log.warn("Invalid role: {}", role);
            }
        }

        Page<User> users = userRepository.findUsersWithFilters(userRole, search, pageable);
        return users.map(this::convertToDTO);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserDetails(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
        return convertToDTO(user);
    }

    public UserDTO updateUserRole(Long id, String role) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        try {
            User.Role newRole = User.Role.valueOf(role.toUpperCase());
            user.setRole(newRole);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        User updatedUser = userRepository.save(user);
        log.info("User {} role updated to: {}", id, role);

        return convertToDTO(updatedUser);
    }

    public UserDTO toggleUserStatus(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setIsActive(!user.getIsActive());
        User updatedUser = userRepository.save(user);

        log.info("User {} status toggled to: {}", id, updatedUser.getIsActive());
        return convertToDTO(updatedUser);
    }

    @Transactional(readOnly = true)
    public Map<String, Long> getUserStats() {
        Map<String, Long> stats = new HashMap<>();

        stats.put("totalUsers", userRepository.count());
        stats.put("totalCustomers", userRepository.countByRole(User.Role.CUSTOMER));
        stats.put("totalAdmins", userRepository.countByRole(User.Role.ADMIN));
        stats.put("totalModerators", userRepository.countByRole(User.Role.MODERATOR));
        stats.put("totalSellers", userRepository.countByRole(User.Role.SELLER));

        stats.put("activeUsers", userRepository.countByIsActiveTrue());
        stats.put("inactiveUsers", userRepository.countByIsActiveFalse());
        stats.put("verifiedEmails", userRepository.countByIsEmailVerifiedTrue());
        stats.put("unverifiedEmails", userRepository.countByIsEmailVerifiedFalse());

        return stats;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRegistrationChartData(int days) {
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusDays(days);

        List<Object[]> results = userRepository.getRegistrationCountByDate(startDate, endDate);

        return results.stream()
                .map(result -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("date", result[0].toString());
                    data.put("count", result[1]);
                    return data;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getRoleDistribution() {
        List<Object[]> results = userRepository.countByRole();

        return results.stream()
                .map(result -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("role", result[0].toString());
                    data.put("count", result[1]);
                    return data;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDTO> searchUsers(String query) {
        List<User> users = userRepository.searchUsers(query, Pageable.ofSize(20));
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getTopCustomers(int limit) {
        List<Object[]> results = userRepository.findTopCustomers(Pageable.ofSize(limit));

        return results.stream()
                .map(result -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("userId", result[0]);
                    data.put("firstName", result[1]);
                    data.put("lastName", result[2]);
                    data.put("email", result[3]);
                    data.put("orderCount", result[4]);
                    data.put("totalSpent", result[5]);
                    return data;
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDTO> getInactiveUsers(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        List<User> users = userRepository.findInactiveUsers(cutoffDate, Pageable.unpaged());
        return users.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setFullName(user.getFullName());
        dto.setPhone(user.getPhone());
        dto.setAddress(user.getAddress());
        dto.setCity(user.getCity());
        dto.setState(user.getState());
        dto.setZipCode(user.getZipCode());
        dto.setCountry(user.getCountry());
        dto.setRole(user.getRole());
        dto.setIsActive(user.getIsActive());
        dto.setIsEmailVerified(user.getIsEmailVerified());
        dto.setCreatedAt(user.getCreatedAt());
        dto.setLastLogin(user.getLastLogin());
        dto.setProfileImageUrl(user.getProfileImageUrl());
        dto.setPreferredLanguage(user.getPreferredLanguage());
        dto.setCurrency(user.getCurrency());
        dto.setNewsletterSubscribed(user.getNewsletterSubscribed());

        // Calculate additional fields
        if (user.getOrders() != null) {
            dto.setTotalOrders(user.getOrders().size());

            double totalSpent = user.getOrders().stream()
                    .filter(order -> order.getStatus() == Order.OrderStatus.DELIVERED)
                    .mapToDouble(order -> order.getTotalAmount().doubleValue())
                    .sum();
            dto.setTotalSpent(totalSpent);
        }

        return dto;
    }
}