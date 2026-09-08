package com.campus.dorm.util;

import com.campus.dorm.common.DormConstants;
import com.campus.dorm.entity.SysUser;
import com.campus.dorm.security.DormUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {
    private SecurityUtil() {}

    public static SysUser getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof DormUserDetails details) {
            return details.getUser();
        }
        return null;
    }

    public static Long getCurrentUserId() {
        SysUser user = getCurrentUser();
        return user == null ? null : user.getId();
    }

    public static boolean isAdmin() {
        return isAdmin(getCurrentUser());
    }

    public static boolean isAdmin(SysUser user) {
        return user != null && DormConstants.ROLE_ADMIN.equalsIgnoreCase(user.getRole());
    }

    public static boolean isAuthenticated() {
        return getCurrentUser() != null;
    }

    public static SysUser toSafeUser(SysUser user) {
        if (user == null) {
            return null;
        }
        SysUser safe = new SysUser();
        safe.setId(user.getId());
        safe.setUsername(user.getUsername());
        safe.setRealName(user.getRealName());
        safe.setPhone(user.getPhone());
        safe.setEmail(user.getEmail());
        safe.setAvatar(user.getAvatar());
        safe.setRole(user.getRole());
        safe.setStatus(user.getStatus());
        safe.setCreateTime(user.getCreateTime());
        return safe;
    }
}
