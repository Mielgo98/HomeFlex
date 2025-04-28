// src/main/java/com/example/demo/chatbot/model/EntityTypeConverter.java
package com.example.demo.chatbot.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class EntityTypeConverter implements AttributeConverter<EntityType, String> {
    @Override
    public String convertToDatabaseColumn(EntityType attribute) {
        return attribute != null ? attribute.getCode() : null;
    }
    @Override
    public EntityType convertToEntityAttribute(String dbData) {
        return EntityType.fromCode(dbData);
    }
}
