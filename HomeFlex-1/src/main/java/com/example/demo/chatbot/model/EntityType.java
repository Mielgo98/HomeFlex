package com.example.demo.chatbot.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EntityType {
    PROPERTY("property", "Propiedades inmobiliarias"),
    RESERVATION("reservation", "Reservas"),
    USER("user", "Usuarios"),
    REVIEW("review", "Valoraciones"),
    PAYMENT("payment", "Pagos"),
    ALL("all", "Todas las entidades");

    private static final Logger logger = LoggerFactory.getLogger(EntityType.class);
    
    private final String code;
    private final String description;

    EntityType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Este valor se usará al serializar a JSON.
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Jackson llamará a este método para convertir el string JSON
     * (por ejemplo "property") en el enum correspondiente.
     */
    @JsonCreator
    public static EntityType fromCode(String code) {
        logger.debug("Convirtiendo code '{}' a EntityType", code);
        
        if (code == null || code.trim().isEmpty()) {
            logger.debug("Code es null o vacío, retornando valor por defecto: PROPERTY");
            return PROPERTY;
        }
        
        for (EntityType type : values()) {
            if (type.code.equalsIgnoreCase(code.trim())) {
                logger.debug("Code '{}' convertido a EntityType: {}", code, type);
                return type;
            }
        }
        
        logger.warn("No se encontró EntityType para code '{}', retornando valor por defecto: PROPERTY", code);
        return PROPERTY;
    }
    
    @Override
    public String toString() {
        return code + " (" + description + ")";
    }
}