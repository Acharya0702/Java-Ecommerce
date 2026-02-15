package com.ecommerce.ecommercebackend.security;

import com.ecommerce.ecommercebackend.entity.User;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@AllArgsConstructor
@Slf4j
public class CustomUserDetails implements UserDetails {

    private User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        boolean isActive = user.getIsActive() != null && user.getIsActive();
        log.debug("AccountNonLocked check for {}: {}", user.getEmail(), isActive);
        return isActive;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        boolean isEnabled = (user.getIsActive() != null && user.getIsActive()) &&
                (user.getIsEmailVerified() != null && user.getIsEmailVerified());
        log.debug("Enabled check for {}: isActive={}, isEmailVerified={}, result={}",
                user.getEmail(), user.getIsActive(), user.getIsEmailVerified(), isEnabled);
        return isEnabled;
    }

    public Long getUserId() {
        return user.getId();
    }

    public String getFullName() {
        return user.getFirstName() + " " + user.getLastName();
    }

    public User.Role getRole() {
        return user.getRole();
    }
}