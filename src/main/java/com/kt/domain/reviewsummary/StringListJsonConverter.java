package com.kt.domain.reviewsummary;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Collections;
import java.util.List;

@Converter
public class StringListJsonConverter implements AttributeConverter<List<String>, String> {
    private static final ObjectMapper om = new ObjectMapper();
    private static final TypeReference<List<String>> LIST = new TypeReference<>() {};

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        try {
            return om.writeValueAsString(attribute == null ? Collections.emptyList() : attribute);
        } catch (Exception e) {
            throw new IllegalStateException("List -> JSON 변환 실패", e);
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        try {
            if (dbData == null || dbData.isBlank()) return Collections.emptyList();
            return om.readValue(dbData, LIST);
        } catch (Exception e) {
            throw new IllegalStateException("JSON -> List 변환 실패", e);
        }
    }
}