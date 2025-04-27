package com.example.demo.propiedad.event;

public class PropiedadCreatedEvent {
    private final Long propiedadId;
    
    public PropiedadCreatedEvent(Long propiedadId) {
        this.propiedadId = propiedadId;
    }
    
    public Long getPropiedadId() {
        return propiedadId;
    }
}