package com.example.demo.propiedad.model;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para las estadísticas de propiedades de un propietario
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PropiedadEstadisticasDTO {
    
    // Estadísticas generales
    private int totalPropiedades;
    private int propiedadesActivas;
    private int propiedadesInactivas;
    
    // Estadísticas por ubicación
    private Map<String, Integer> propiedadesPorCiudad;
    
    // Estadísticas temporales
    private Map<String, Integer> publicacionesPorMes;
    
 
}