package com.shoerp.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.shoerp.common.BusinessException;
import com.shoerp.common.MenuCatalog;
import com.shoerp.common.StatusCatalog;
import com.shoerp.entity.RoleMenu;
import com.shoerp.repository.RoleMenuRepository;

@Service
public class PermissionService {

    public static final List<String> ROLES = StatusCatalog.ROLES;

    private static final String NONE_MARKER = "__none__";

    private static final Map<String, List<String>> DEFAULT_MENUS = defaultMenus();

    private final RoleMenuRepository roleMenuRepository;

    public PermissionService(RoleMenuRepository roleMenuRepository) {
        this.roleMenuRepository = roleMenuRepository;
    }

    private static Map<String, List<String>> defaultMenus() {
        Map<String, List<String>> map = new LinkedHashMap<>();
        map.put("ADMIN", MenuCatalog.allKeys());
        map.put("SALES", List.of("dashboard", "shoes", "customers", "quotations", "orders", "shipments"));
        map.put("OPS", List.of("dashboard", "shoes", "suppliers", "orders", "shipments", "purchases"));
        map.put("FINANCE", List.of("dashboard", "customers", "orders", "shipments", "finance", "fx"));
        return map;
    }

    @Transactional
    public void seedDefaults() {
        DEFAULT_MENUS.forEach((role, keys) -> {
            if (roleMenuRepository.findByRole(role).isEmpty()) {
                keys.forEach(key -> roleMenuRepository.save(new RoleMenu(role, key)));
            }
        });
    }

    @Transactional(readOnly = true)
    public List<String> menusOf(String role) {
        if (role == null || role.isBlank()) {
            return List.of();
        }
        List<RoleMenu> rows = roleMenuRepository.findByRole(role);
        if (rows.isEmpty()) {
            return DEFAULT_MENUS.getOrDefault(role, List.of());
        }
        return rows.stream()
                .map(RoleMenu::getMenuKey)
                .filter(MenuCatalog::exists)
                .distinct()
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<String, List<String>> all() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String role : ROLES) {
            result.put(role, menusOf(role));
        }
        return result;
    }

    @Transactional
    public List<String> save(String role, List<String> menuKeys) {
        if (!ROLES.contains(role)) {
            throw new BusinessException(404, "Role not found");
        }
        List<String> keys = menuKeys == null ? List.of() : menuKeys.stream()
                .filter(MenuCatalog::exists)
                .distinct()
                .toList();
        roleMenuRepository.deleteByRole(role);
        roleMenuRepository.flush();
        if (keys.isEmpty()) {
            roleMenuRepository.save(new RoleMenu(role, NONE_MARKER));
        } else {
            keys.forEach(key -> roleMenuRepository.save(new RoleMenu(role, key)));
        }
        return keys;
    }
}
