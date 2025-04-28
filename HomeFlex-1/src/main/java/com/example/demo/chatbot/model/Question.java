package com.example.demo.chatbot.model;

public record Question(String question, EntityType entityType) {
    // Constructor conveniente que acepta String y lo convierte a EntityType
    public Question(String question, String entityTypeCode) {
        this(question, parseEntityType(entityTypeCode));
    }
    
    private static EntityType parseEntityType(String entityTypeCode) {
        if (entityTypeCode == null || entityTypeCode.trim().isEmpty()) {
            return EntityType.PROPERTY; // Valor por defecto
        }
        try {
            return EntityType.fromCode(entityTypeCode);
        } catch (Exception e) {
            return EntityType.PROPERTY; // En caso de error, usar el valor por defecto
        }
    }
}