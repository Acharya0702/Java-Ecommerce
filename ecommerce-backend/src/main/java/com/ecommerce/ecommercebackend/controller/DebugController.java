package com.ecommerce.ecommercebackend.controller;

import com.ecommerce.ecommercebackend.entity.User;
import com.ecommerce.ecommercebackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/debug")
@RequiredArgsConstructor
public class DebugController {

    private final UserRepository userRepository;

    @GetMapping("/user/{email}")
    public ResponseEntity<?> checkUserStatus(@PathVariable String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    Map<String, Object> status = new HashMap<>();
                    status.put("email", user.getEmail());
                    status.put("isActive", user.getIsActive());
                    status.put("isEmailVerified", user.getIsEmailVerified());
                    status.put("role", user.getRole());
                    status.put("emailVerificationToken", user.getEmailVerificationToken() != null ? "Present" : "Null");
                    status.put("canLogin", user.getIsActive() && user.getIsEmailVerified());
                    return ResponseEntity.ok(status);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/user/{email}/activate")
    public ResponseEntity<?> activateUser(@PathVariable String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    user.setIsActive(true);
                    userRepository.save(user);
                    return ResponseEntity.ok("User activated successfully");
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/user/{email}/verify")
    public ResponseEntity<?> verifyUserEmail(@PathVariable String email) {
        return userRepository.findByEmail(email)
                .map(user -> {
                    user.setIsEmailVerified(true);
                    user.setEmailVerificationToken(null);
                    userRepository.save(user);
                    return ResponseEntity.ok("Email verified successfully");
                })
                .orElse(ResponseEntity.notFound().build());
    }
}