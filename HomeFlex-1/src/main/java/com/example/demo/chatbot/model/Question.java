package com.example.demo.chatbot.model;

public record Question(String question, EntityType entityType) {
    // Constructor conveniente que acepta String y lo convierte a EntityType
    public Question(String question, String entityTypeCode) {
        this(question, EntityType.fromCode(entityTypeCode));
    }
}