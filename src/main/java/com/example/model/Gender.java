package com.example.model;

import java.text.Normalizer;
import java.util.Locale;

public enum Gender {
    MALE, FEMALE, OTHER;

    public static Gender fromValue(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = normalize(value);
        switch (normalized) {
            case "male":
            case "m":
            case "nam":
                return MALE;
            case "female":
            case "f":
            case "nu":
                return FEMALE;
            case "other":
            case "o":
            case "khac":
                return OTHER;
            default:
                throw new IllegalArgumentException("Unsupported gender value: " + value);
        }
    }

    private static String normalize(String value) {
        String withoutAccents = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT);
    }
}
