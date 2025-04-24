package com.example.demo.reserva.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.reserva.model.EstadoReserva;
import com.example.demo.reserva.model.ReservaVO;
import com.example.demo.usuario.model.UsuarioVO;

@Repository
public interface ReservaRepository extends JpaRepository<ReservaVO, Long> {
    
    // Buscar reservas por código
    Optional<ReservaVO> findByCodigoReserva(String codigoReserva);
    
    // Buscar reservas de un usuario (inquilino)
    List<ReservaVO> findByUsuario(UsuarioVO usuario);
    
    // Buscar reservas de un usuario paginadas
    Page<ReservaVO> findByUsuario(UsuarioVO usuario, Pageable pageable);
    
    // Buscar reservas por estado para un usuario
    List<ReservaVO> findByUsuarioAndEstado(UsuarioVO usuario, EstadoReserva estado);
    
    // Buscar reservas por estado para un usuario paginadas
    Page<ReservaVO> findByUsuarioAndEstado(UsuarioVO usuario, EstadoReserva estado, Pageable pageable);
    
    // Buscar reservas para una propiedad
    List<ReservaVO> findByPropiedad(PropiedadVO propiedad);
    
    // Buscar reservas para una propiedad paginadas
    Page<ReservaVO> findByPropiedad(PropiedadVO propiedad, Pageable pageable);
    
    // Buscar reservas por estado para una propiedad
    List<ReservaVO> findByPropiedadAndEstado(PropiedadVO propiedad, EstadoReserva estado);
    
    // Buscar reservas para un propietario
    @Query("SELECT r FROM ReservaVO r WHERE r.propiedad.propietario = :propietario")
    List<ReservaVO> findByPropietario(@Param("propietario") UsuarioVO propietario);
    
    // Buscar reservas para un propietario paginadas
    @Query("SELECT r FROM ReservaVO r WHERE r.propiedad.propietario = :propietario")
    Page<ReservaVO> findByPropietario(@Param("propietario") UsuarioVO propietario, Pageable pageable);
    
    // Buscar reservas por estado para un propietario
    @Query("SELECT r FROM ReservaVO r WHERE r.propiedad.propietario = :propietario AND r.estado = :estado")
    List<ReservaVO> findByPropietarioAndEstado(
            @Param("propietario") UsuarioVO propietario, 
            @Param("estado") EstadoReserva estado);
    
    // Buscar reservas por estado para un propietario paginadas
    @Query("SELECT r FROM ReservaVO r WHERE r.propiedad.propietario = :propietario AND r.estado = :estado")
    Page<ReservaVO> findByPropietarioAndEstado(
            @Param("propietario") UsuarioVO propietario, 
            @Param("estado") EstadoReserva estado,
            Pageable pageable);
    
    // Buscar reservas activas (no canceladas) para un rango de fechas y una propiedad
    @Query("SELECT r FROM ReservaVO r WHERE r.propiedad = :propiedad " +
           "AND r.estado != 'CANCELADA' " +
           "AND ((r.fechaInicio BETWEEN :inicio AND :fin) " +
           "OR (r.fechaFin BETWEEN :inicio AND :fin) " +
           "OR (:inicio BETWEEN r.fechaInicio AND r.fechaFin) " +
           "OR (:fin BETWEEN r.fechaInicio AND r.fechaFin))")
    List<ReservaVO> findReservasActivasEnRango(
            @Param("propiedad") PropiedadVO propiedad,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin);
    
    // Verificar disponibilidad para un rango de fechas y una propiedad
    @Query("SELECT COUNT(r) > 0 FROM ReservaVO r WHERE r.propiedad = :propiedad " +
           "AND r.estado != 'CANCELADA' " +
           "AND ((r.fechaInicio BETWEEN :inicio AND :fin) " +
           "OR (r.fechaFin BETWEEN :inicio AND :fin) " +
           "OR (:inicio BETWEEN r.fechaInicio AND r.fechaFin) " +
           "OR (:fin BETWEEN r.fechaInicio AND r.fechaFin))")
    boolean existsReservaActivaEnRango(
            @Param("propiedad") PropiedadVO propiedad,
            @Param("inicio") LocalDate inicio,
            @Param("fin") LocalDate fin);
    
    // Obtener estadísticas por mes para una propiedad
    @Query("SELECT FUNCTION('MONTH', r.fechaInicio) as mes, COUNT(r) " +
           "FROM ReservaVO r " +
           "WHERE r.propiedad = :propiedad " +
           "AND r.estado != 'CANCELADA' " +
           "AND r.fechaInicio >= :inicio " +
           "GROUP BY FUNCTION('MONTH', r.fechaInicio)")
    List<Object[]> countReservasPorMesParaPropiedad(
            @Param("propiedad") PropiedadVO propiedad,
            @Param("inicio") LocalDate inicio);
    
    // Obtener estadísticas por mes para un propietario
    @Query("SELECT FUNCTION('MONTH', r.fechaInicio) as mes, COUNT(r) " +
           "FROM ReservaVO r " +
           "WHERE r.propiedad.propietario = :propietario " +
           "AND r.estado != 'CANCELADA' " +
           "AND r.fechaInicio >= :inicio " +
           "GROUP BY FUNCTION('MONTH', r.fechaInicio)")
    List<Object[]> countReservasPorMesParaPropietario(
            @Param("propietario") UsuarioVO propietario,
            @Param("inicio") LocalDate inicio);
}