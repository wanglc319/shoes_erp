package com.shoerp.security;

import java.util.List;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

import com.shoerp.service.PermissionService;

public final class MenuAuthorizationManagers {

    private MenuAuthorizationManagers() {
    }

    public static AuthorizationManager<RequestAuthorizationContext> menu(String menuKey, PermissionService permissionService) {
        return (authenticationSupplier, context) -> {
            Authentication authentication = authenticationSupplier.get();
            if (authentication == null || !authentication.isAuthenticated()) {
                return new AuthorizationDecision(false);
            }
            String role = roleOf(authentication);
            if ("ADMIN".equals(role)) {
                return new AuthorizationDecision(true);
            }
            return new AuthorizationDecision(permissionService.menusOf(role).contains(menuKey));
        };
    }

    private static String roleOf(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .toList();
        return roles.isEmpty() ? "" : roles.get(0);
    }
}
