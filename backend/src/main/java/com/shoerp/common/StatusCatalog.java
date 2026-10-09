package com.shoerp.common;

import java.util.List;

public final class StatusCatalog {

    public static final List<String> SHOE = List.of("ACTIVE", "INACTIVE", "DISCONTINUED");

    public static final List<String> QUOTATION = List.of("DRAFT", "SENT", "WON", "LOST");

    public static final List<String> ORDER = List.of("PENDING", "CONFIRMED", "PARTIAL_SHIPPED", "SHIPPED", "CANCELLED");

    public static final List<String> PRODUCTION = List.of("NOT_STARTED", "IN_PRODUCTION", "COMPLETED");

    public static final List<String> SHIPMENT = List.of("PREPARING", "DEPARTED", "ARRIVED", "CLEARED", "DELIVERED");

    public static final List<String> PURCHASE = List.of("DRAFT", "CONFIRMED", "IN_PRODUCTION", "DELIVERED", "CANCELLED");

    public static final List<String> ROLES = List.of("ADMIN", "SALES", "OPS", "FINANCE");

    private StatusCatalog() {
    }

    public static String require(String value, List<String> allowed, String field) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(field + " is required");
        }
        String normalized = value.trim().toUpperCase();
        if (!allowed.contains(normalized)) {
            throw new BusinessException(field + " must be one of " + String.join(", ", allowed));
        }
        return normalized;
    }

    public static String requireRole(String role) {
        return require(role, ROLES, "Role");
    }
}
