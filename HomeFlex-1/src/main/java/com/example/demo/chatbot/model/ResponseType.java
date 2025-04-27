package com.example.demo.chatbot.model;

public enum ResponseType {
    SUCCESS("success", "Respuesta exitosa con información encontrada"),
    NO_INFORMATION("no_info", "No se encontró información relacionada"),
    ERROR("error", "Error al procesar la consulta"),
    CLARIFICATION("clarification", "Se necesita más información para responder");
    
    private final String code;
    private final String description;
    
    ResponseType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
}