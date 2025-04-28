package com.example.demo.pago.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.pago.model.EstadoPago;
import com.example.demo.pago.model.PagoVO;
import com.example.demo.reserva.model.ReservaVO;
import com.example.demo.usuario.model.UsuarioVO;

@Repository
public interface PagoRepository extends JpaRepository<PagoVO, Long> {
    
    // Buscar por ID de sesión
    Optional<PagoVO> findByIdSesion(String idSesion);
    
    // Buscar por ID de pago
    Optional<PagoVO> findByIdPago(String idPago);
    
    // Buscar pagos por reserva
    List<PagoVO> findByReserva(ReservaVO reserva);
    
    // Buscar pagos por reserva y estado
    List<PagoVO> findByReservaAndEstado(ReservaVO reserva, EstadoPago estado);
    
    // Buscar pagos por usuario
    List<PagoVO> findByUsuario(UsuarioVO usuario);
    
    // Buscar pagos por estado
    List<PagoVO> findByEstado(EstadoPago estado);
    
    // Comprobar si existe un pago completado para una reserva
    boolean existsByReservaAndEstado(ReservaVO reserva, EstadoPago estado);
    
    // Obtener el último pago para una reserva
    Optional<PagoVO> findFirstByReservaOrderByFechaCreacionDesc(ReservaVO reserva);
    
    // Consulta avanzada para buscar pagos con filtros
    @Query("SELECT p FROM PagoVO p WHERE " +
           "(:reservaId IS NULL OR p.reserva.id = :reservaId) AND " +
           "(:estado IS NULL OR p.estado = :estado) AND " +
           "(:metodoPago IS NULL OR p.metodoPago = :metodoPago) AND " +
           "(:idPago IS NULL OR p.idPago = :idPago)")
    List<PagoVO> findByFilters(
            @Param("reservaId") Long reservaId,
            @Param("estado") EstadoPago estado,
            @Param("metodoPago") String metodoPago,
            @Param("idPago") String idPago);
    
    // Estadísticas: Total de pagos por estado
    @Query("SELECT p.estado, COUNT(p) FROM PagoVO p GROUP BY p.estado")
    List<Object[]> countByEstadoGrouped();
    
    // Estadísticas: Total de pagos por método de pago
    @Query("SELECT p.metodoPago, COUNT(p) FROM PagoVO p GROUP BY p.metodoPago")
    List<Object[]> countByMetodoPagoGrouped();
    
    // Estadísticas: Suma total por estado
    @Query("SELECT p.estado, SUM(p.monto) FROM PagoVO p GROUP BY p.estado")
    List<Object[]> sumMontoByEstado();
}