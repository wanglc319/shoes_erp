package com.shoerp.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户上下文。JWT 过滤器中 principal 即 userId，权限为 ROLE_{role}。
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long userId)) {
            return null;
        }
        return userId;
    }

    public static String role() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getAuthorities() == null) {
            return null;
        }
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a != null && a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse(null);
    }

    public static boolean isAdmin() {
        return "ADMIN".equals(role());
    }

    public static boolean isFinance() {
        return "FINANCE".equals(role());
    }

    /** 管理员可见全公司数据；其他人只能看到自己创建的数据。无认证上下文视为系统内部调用。 */
    public static boolean owns(Long ownerId) {
        Long me = id();
        if (me == null || isAdmin()) {
            return true;
        }
        return me.equals(ownerId);
    }

    /** 财务模块：管理员与财务角色可查看全部；系统内部调用放行。 */
    public static boolean canViewAllFinance() {
        if (id() == null) {
            return true;
        }
        return isAdmin() || isFinance();
    }

    /** 是否需要按归属人过滤（有登录用户且非管理员）。 */
    public static boolean needsOwnershipFilter() {
        Long me = id();
        return me != null && !isAdmin();
    }
}
