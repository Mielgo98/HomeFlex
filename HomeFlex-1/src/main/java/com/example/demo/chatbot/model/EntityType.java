package com.example.demo.chatbot.model;

public enum EntityType {
    PROPERTY("property", "Propiedades inmobiliarias"),
    RESERVATION("reservation", "Reservas"),
    USER("user", "Usuarios"),
    REVIEW("review", "Valoraciones"),
    PAYMENT("payment", "Pagos"),
    ALL("all", "Todas las entidades");
    
    private final String code;
    private final String description;
    
    EntityType(String code, String description) {
        this.code = code;
        this.description = description;
    }
    
    public String getCode() {
        return code;
    }
    
    public String getDescription() {
        return description;
    }
    
    public static EntityType fromCode(String code) {
        if (code == null) return ALL;
        
        for (EntityType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return ALL;
    }
}