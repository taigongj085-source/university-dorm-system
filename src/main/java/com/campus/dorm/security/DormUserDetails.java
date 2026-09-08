package com.campus.dorm.security;

import com.campus.dorm.common.DormConstants;
import com.campus.dorm.entity.SysUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class DormUserDetails implements UserDetails {

    private final SysUser user;

    public DormUserDetails(SysUser user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = user == null || user.getRole() == null ? DormConstants.ROLE_STUDENT : user.getRole();
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() {
        return user == null ? null : user.getPassword();
    }

    @Override
    public String getUsername() {
        return user == null ? null : user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user == null || user.getStatus() == null || user.getStatus() == 1;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user != null && user.getStatus() != null && user.getStatus() == 1;
    }
}
