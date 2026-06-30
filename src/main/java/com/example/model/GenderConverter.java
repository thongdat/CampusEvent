package com.example.model;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender gender) {
        return gender != null ? gender.name() : null;
    }

    @Override
    public Gender convertToEntityAttribute(String value) {
        return Gender.fromValue(value);
    }
}
