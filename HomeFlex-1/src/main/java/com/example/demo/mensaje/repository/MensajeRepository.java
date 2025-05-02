package com.example.demo.mensaje.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.mensaje.model.MensajeVO;
import com.example.demo.usuario.model.UsuarioVO;

import java.util.List;

@Repository
public interface MensajeRepository extends JpaRepository<MensajeVO, Long> {
    
    @Query("SELECT m FROM MensajeVO m WHERE (m.emisor.id = :usuarioId AND m.receptor.id = :otroUsuarioId) OR (m.emisor.id = :otroUsuarioId AND m.receptor.id = :usuarioId) ORDER BY m.fechaEnvio ASC")
    List<MensajeVO> findConversacion(@Param("usuarioId") Long usuarioId, @Param("otroUsuarioId") Long otroUsuarioId);
    
    @Query("SELECT m FROM MensajeVO m WHERE (m.emisor.id = :usuarioId AND m.receptor.id = :otroUsuarioId AND m.propiedad.id = :propiedadId) OR (m.emisor.id = :otroUsuarioId AND m.receptor.id = :usuarioId AND m.propiedad.id = :propiedadId) ORDER BY m.fechaEnvio ASC")
    List<MensajeVO> findConversacionByPropiedad(@Param("usuarioId") Long usuarioId, @Param("otroUsuarioId") Long otroUsuarioId, @Param("propiedadId") Long propiedadId);
    
    @Query("SELECT DISTINCT m.receptor FROM MensajeVO m WHERE m.emisor.id = :usuarioId")
    List<UsuarioVO> findReceptores(@Param("usuarioId") Long usuarioId);

    @Query("SELECT DISTINCT m.emisor FROM MensajeVO m WHERE m.receptor.id = :usuarioId")
    List<UsuarioVO> findEmisores(@Param("usuarioId") Long usuarioId);
    
    @Query("SELECT COUNT(m) FROM MensajeVO m WHERE m.receptor.id = :usuarioId AND m.leido = false")
    Integer countUnreadMessages(@Param("usuarioId") Long usuarioId);
    
    @Query("SELECT COUNT(m) FROM MensajeVO m WHERE m.receptor.id = :usuarioId AND m.emisor.id = :emisorId AND m.leido = false")
    Integer countUnreadMessagesFromUser(@Param("usuarioId") Long usuarioId, @Param("emisorId") Long emisorId);
    
    @Modifying
    @Query("UPDATE MensajeVO m SET m.leido = true WHERE m.receptor.id = :usuarioId AND m.emisor.id = :emisorId AND m.leido = false")
    void markAsRead(@Param("usuarioId") Long usuarioId, @Param("emisorId") Long emisorId);
    
    @Query("SELECT m FROM MensajeVO m WHERE (m.emisor.id = :usuarioId OR m.receptor.id = :usuarioId) ORDER BY m.fechaEnvio DESC")
    List<MensajeVO> findAllByUsuarioOrderByFechaEnvioDesc(@Param("usuarioId") Long usuarioId);
}