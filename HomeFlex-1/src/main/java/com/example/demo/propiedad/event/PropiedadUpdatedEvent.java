package com.example.demo.propiedad.event;

public class PropiedadUpdatedEvent {
    private final Long propiedadId;
    
    public PropiedadUpdatedEvent(Long propiedadId) {
        this.propiedadId = propiedadId;
    }
    
    public Long getPropiedadId() {
        return propiedadId;
    }
}