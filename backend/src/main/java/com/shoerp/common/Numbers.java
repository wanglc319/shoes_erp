package com.shoerp.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Numbers {

    private Numbers() {
    }

    public static BigDecimal scale(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal nonNegative(BigDecimal value, String field) {
        BigDecimal scaled = scale(value);
        if (scaled.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(field + " must not be negative");
        }
        return scaled;
    }

    public static BigDecimal nonNegativeOrNull(BigDecimal value, String field) {
        if (value == null) {
            return null;
        }
        return nonNegative(value, field);
    }

    public static int nonNegative(Integer value, String field) {
        int raw = value == null ? 0 : value;
        if (raw < 0) {
            throw new BusinessException(field + " must not be negative");
        }
        return raw;
    }

    public static Integer nonNegativeOrNull(Integer value, String field) {
        if (value == null) {
            return null;
        }
        return nonNegative(value, field);
    }
}
