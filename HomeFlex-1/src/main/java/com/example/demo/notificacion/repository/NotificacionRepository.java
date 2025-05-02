package com.example.demo.notificacion.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.notificacion.model.NotificacionVO;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<NotificacionVO, Long> {
    
    List<NotificacionVO> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);
    
    @Query("SELECT COUNT(n) FROM NotificacionVO n WHERE n.usuario.id = :usuarioId AND n.leida = false")
    Integer countUnreadNotifications(@Param("usuarioId") Long usuarioId);
    
    List<NotificacionVO> findByUsuarioIdAndTipoOrderByFechaCreacionDesc(Long usuarioId, String tipo);
}