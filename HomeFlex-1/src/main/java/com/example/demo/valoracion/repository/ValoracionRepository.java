package com.example.demo.valoracion.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.valoracion.model.ValoracionVO;

@Repository
public interface ValoracionRepository extends JpaRepository<ValoracionVO, Long> {
    
    // Buscar valoraciones por propiedad
    List<ValoracionVO> findByPropiedad(PropiedadVO propiedad);
    
    // Buscar valoraciones aprobadas por propiedad
    List<ValoracionVO> findByPropiedadAndAprobadaTrue(PropiedadVO propiedad);
    
    // Buscar valoraciones aprobadas por propiedad con paginación
    Page<ValoracionVO> findByPropiedadAndAprobadaTrue(PropiedadVO propiedad, Pageable pageable);
    
    // Buscar valoraciones por usuario
    List<ValoracionVO> findByUsuario(UsuarioVO usuario);
    
    // Buscar valoraciones por propiedad y usuario
    Optional<ValoracionVO> findByPropiedadAndUsuario(PropiedadVO propiedad, UsuarioVO usuario);
    
    // Buscar valoraciones por propiedad que estén aprobadas
    Page<ValoracionVO> findByPropiedadAndAprobada(PropiedadVO propiedad, Boolean aprobada, Pageable pageable);
    
    // Buscar valoraciones por propietario (propiedades de un propietario)
    @Query("SELECT v FROM ValoracionVO v WHERE v.propiedad.propietario = :propietario")
    List<ValoracionVO> findByPropietario(@Param("propietario") UsuarioVO propietario);
    
    // Buscar valoraciones por propietario y estado de aprobación
    @Query("SELECT v FROM ValoracionVO v WHERE v.propiedad.propietario = :propietario AND v.aprobada = :aprobada")
    List<ValoracionVO> findByPropietarioAndAprobada(
            @Param("propietario") UsuarioVO propietario, 
            @Param("aprobada") Boolean aprobada);
    
    // Buscar valoraciones por propietario con paginación
    @Query("SELECT v FROM ValoracionVO v WHERE v.propiedad.propietario = :propietario")
    Page<ValoracionVO> findByPropietario(@Param("propietario") UsuarioVO propietario, Pageable pageable);
    
    // Contar valoraciones por propiedad
    long countByPropiedad(PropiedadVO propiedad);
    
    // Contar valoraciones aprobadas por propiedad
    long countByPropiedadAndAprobadaTrue(PropiedadVO propiedad);
    
    // Calcular la puntuación media por propiedad
    @Query("SELECT AVG(v.puntuacion) FROM ValoracionVO v WHERE v.propiedad = :propiedad AND v.aprobada = true")
    Double calcularPuntuacionMediaPropiedad(@Param("propiedad") PropiedadVO propiedad);
    
    // Calcular la puntuación media para cada aspecto por propiedad
    @Query("SELECT AVG(v.limpieza) FROM ValoracionVO v WHERE v.propiedad = :propiedad AND v.aprobada = true")
    Double calcularPuntuacionMediaLimpieza(@Param("propiedad") PropiedadVO propiedad);
    
    @Query("SELECT AVG(v.ubicacion) FROM ValoracionVO v WHERE v.propiedad = :propiedad AND v.aprobada = true")
    Double calcularPuntuacionMediaUbicacion(@Param("propiedad") PropiedadVO propiedad);
    
    @Query("SELECT AVG(v.comunicacion) FROM ValoracionVO v WHERE v.propiedad = :propiedad AND v.aprobada = true")
    Double calcularPuntuacionMediaComunicacion(@Param("propiedad") PropiedadVO propiedad);
    
    @Query("SELECT AVG(v.calidad) FROM ValoracionVO v WHERE v.propiedad = :propiedad AND v.aprobada = true")
    Double calcularPuntuacionMediaCalidad(@Param("propiedad") PropiedadVO propiedad);
}