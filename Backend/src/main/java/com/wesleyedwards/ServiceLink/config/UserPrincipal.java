package com.wesleyedwards.ServiceLink.config;

import com.wesleyedwards.ServiceLink.entities.User;
import com.wesleyedwards.ServiceLink.enums.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {
    private final User user;

    public UUID getUserId() { return user.getUserId(); }

    @Override
    public String getUsername() {
        return user.getCredentials().getUsername();
    }

    @Override
    public String getPassword() {
        return user.getCredentials().getPassword();
    }

    @Override
    public boolean isEnabled() {
        return !user.isDisabled();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    public Role getRole() { return user.getRole(); }

    // in UserPrincipal
    public boolean isAdmin() { return user.getRole() == Role.ADMIN; }
    public boolean isStaff() { return user.getRole() == Role.ADMIN || user.getRole() == Role.AGENT; }
}
