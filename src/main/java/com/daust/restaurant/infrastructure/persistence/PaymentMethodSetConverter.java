package com.daust.restaurant.infrastructure.persistence;

import com.daust.restaurant.domain.PaymentMethod;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

@Converter
class PaymentMethodSetConverter implements AttributeConverter<Set<PaymentMethod>, String> {

    @Override
    public String convertToDatabaseColumn(Set<PaymentMethod> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return "";
        }
        return attribute.stream()
                .map(Enum::name)
                .sorted()
                .collect(Collectors.joining(","));
    }

    @Override
    public Set<PaymentMethod> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return EnumSet.noneOf(PaymentMethod.class);
        }
        return Arrays.stream(dbData.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(PaymentMethod::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(PaymentMethod.class)));
    }
}
