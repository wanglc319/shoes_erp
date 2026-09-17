package com.shoerp.common;

import java.util.List;

public final class MenuCatalog {

    public record MenuDef(String key, String path, String titleKey) {
    }

    private static final List<MenuDef> MENUS = List.of(
            new MenuDef("dashboard", "/dashboard", "dashboard"),
            new MenuDef("shoes", "/shoes", "shoes"),
            new MenuDef("suppliers", "/suppliers", "suppliers"),
            new MenuDef("customers", "/customers", "customers"),
            new MenuDef("quotations", "/quotations", "quotations"),
            new MenuDef("orders", "/orders", "orders"),
            new MenuDef("shipments", "/shipments", "shipments"),
            new MenuDef("purchases", "/purchases", "purchases"),
            new MenuDef("finance", "/finance", "finance"),
            new MenuDef("fx", "/fx", "fx"),
            new MenuDef("users", "/users", "users"),
            new MenuDef("permissions", "/permissions", "permissions"));

    private MenuCatalog() {
    }

    public static List<MenuDef> all() {
        return MENUS;
    }

    public static List<String> allKeys() {
        return MENUS.stream().map(MenuDef::key).toList();
    }

    public static boolean exists(String key) {
        return MENUS.stream().anyMatch(m -> m.key().equals(key));
    }
}
