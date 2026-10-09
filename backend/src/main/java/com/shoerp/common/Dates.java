package com.shoerp.common;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

public final class Dates {

    private Dates() {
    }

    public static LocalDate parse(String value, LocalDate defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String text = value.trim();
        if (text.length() > 10) {
            text = text.substring(0, 10);
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("Invalid date: " + value);
        }
    }
}
