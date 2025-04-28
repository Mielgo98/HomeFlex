package com.example.demo.chatbot.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

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
        if (code == null) {
            return ALL;
        }
        for (EntityType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return ALL;
    }
}
