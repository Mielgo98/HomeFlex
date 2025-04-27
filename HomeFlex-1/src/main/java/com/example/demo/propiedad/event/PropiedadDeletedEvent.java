package com.example.demo.propiedad.event;

public class PropiedadDeletedEvent {
    private final Long propiedadId;
    
    public PropiedadDeletedEvent(Long propiedadId) {
        this.propiedadId = propiedadId;
    }
    
    public Long getPropiedadId() {
        return propiedadId;
    }
}