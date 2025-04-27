package com.example.demo.chatbot.service;

import org.springframework.stereotype.Service;

import com.example.demo.propiedad.model.PropiedadVO;

@Service
public class ChatbotDataService {
    
    /**
     * Convierte una propiedad a texto para el almacenamiento vectorial
     */
    public String convertPropertyToText(PropiedadVO propiedad) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(propiedad.getId()).append("\n");
        sb.append("Título: ").append(propiedad.getTitulo()).append("\n");
        sb.append("Descripción: ").append(propiedad.getDescripcion()).append("\n");
        sb.append("Ubicación: ").append(propiedad.getDireccion())
            .append(", ").append(propiedad.getCiudad())
            .append(", ").append(propiedad.getPais()).append("\n");
        sb.append("Precio por día: ").append(propiedad.getPrecioDia()).append("€\n");
        if (propiedad.getPrecioSemana() != null) {
            sb.append("Precio por semana: ").append(propiedad.getPrecioSemana()).append("€\n");
        }
        sb.append("Capacidad: ").append(propiedad.getCapacidad()).append(" huéspedes\n");
        sb.append("Dormitorios: ").append(propiedad.getDormitorios()).append("\n");
        sb.append("Baños: ").append(propiedad.getBanos()).append("\n");
        sb.append("Estado: ").append(propiedad.getActivo() ? "Disponible" : "No disponible").append("\n");
        
        return sb.toString();
    }
}