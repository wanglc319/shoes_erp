package com.shoerp.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.shoerp.common.ApiResponse;
import com.shoerp.common.MenuCatalog;
import com.shoerp.service.PermissionService;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    public PermissionController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @GetMapping
    public ApiResponse<Map<String, List<String>>> all() {
        return ApiResponse.success(permissionService.all());
    }

    @GetMapping("/catalog")
    public ApiResponse<List<Map<String, String>>> catalog() {
        List<Map<String, String>> menus = MenuCatalog.all().stream().map(menu -> {
            Map<String, String> item = new HashMap<>();
            item.put("key", menu.key());
            item.put("path", menu.path());
            item.put("titleKey", menu.titleKey());
            return item;
        }).toList();
        return ApiResponse.success(menus);
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, List<String>>> me(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .findFirst()
                .orElse("");
        Map<String, List<String>> result = new HashMap<>();
        result.put("menus", permissionService.menusOf(role));
        return ApiResponse.success(result);
    }

    @PutMapping("/{role}")
    public ApiResponse<List<String>> save(@PathVariable String role, @RequestBody List<String> menuKeys) {
        return ApiResponse.success(permissionService.save(role, menuKeys));
    }
}
