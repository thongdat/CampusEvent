package com.example.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class GenderConverterTest {

    private final GenderConverter converter = new GenderConverter();

    @Test
    void readsLegacyVietnameseGenderValues() {
        assertEquals(Gender.MALE, converter.convertToEntityAttribute("Nam"));
        assertEquals(Gender.FEMALE, converter.convertToEntityAttribute("Nữ"));
        assertEquals(Gender.OTHER, converter.convertToEntityAttribute("Khác"));
    }

    @Test
    void readsCanonicalGenderValues() {
        assertEquals(Gender.MALE, converter.convertToEntityAttribute("MALE"));
        assertEquals(Gender.FEMALE, converter.convertToEntityAttribute("FEMALE"));
        assertEquals(Gender.OTHER, converter.convertToEntityAttribute("OTHER"));
    }

    @Test
    void writesCanonicalGenderValues() {
        assertEquals("MALE", converter.convertToDatabaseColumn(Gender.MALE));
        assertEquals("FEMALE", converter.convertToDatabaseColumn(Gender.FEMALE));
        assertEquals("OTHER", converter.convertToDatabaseColumn(Gender.OTHER));
        assertNull(converter.convertToDatabaseColumn(null));
    }
}
