package com.example.demo.valoracion.model;

import lombok.Data;

	/**
     * Clase interna para estadísticas de valoraciones
     */
    @Data
    public class ValoracionesEstadisticasDTO {
        private Long propiedadId;
        private Double puntuacionGeneral;
        private Double puntuacionLimpieza;
        private Double puntuacionUbicacion;
        private Double puntuacionComunicacion;
        private Double puntuacionCalidad;
        private Integer numeroValoraciones;
    }