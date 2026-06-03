package com.app.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.app.support.UserAccountStatus;

public class HrmsUserDetails implements UserDetails {

    private static final long serialVersionUID = 1L;

    private final Integer userId;
    private final String username;
    private final String password;
    private final String email;
    private final String displayName;
    private final Integer isEnabled;
    private final Integer isActive;
    private final Collection<? extends GrantedAuthority> authorities;

    public HrmsUserDetails(UsersSnapshot user, Collection<? extends GrantedAuthority> authorities) {
        this.userId = user.userId();
        this.username = user.userName();
        this.password = user.password();
        this.email = user.email();
        this.displayName = user.userName();
        this.isEnabled = user.isEnabled();
        this.isActive = user.isActive();
        this.authorities = authorities;
    }

    public Integer getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Integer getIsEnabled() {
        return isEnabled;
    }

    public Integer getIsActive() {
        return isActive;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return UserAccountStatus.canAuthenticate(isEnabled);
    }

    public record UsersSnapshot(Integer userId, String userName, String password, String email,
            Integer isEnabled, Integer isActive) {
    }
}
