// PropiedadRepository.java
package com.example.demo.propiedad.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.usuario.model.UsuarioVO;

@Repository
public interface PropiedadRepository extends JpaRepository<PropiedadVO, Long> {
    
    // Buscar propiedades activas
    List<PropiedadVO> findByActivoTrue();
    
    // Paginar propiedades activas
    Page<PropiedadVO> findByActivoTrue(Pageable pageable);
    
    // Buscar propiedades de un propietario
    List<PropiedadVO> findByPropietario(UsuarioVO propietario);
    
    // Buscar propiedades por ciudad
    List<PropiedadVO> findByCiudadContainingIgnoreCaseAndActivoTrue(String ciudad);
    
    // Búsqueda avanzada
    @Query("SELECT p FROM PropiedadVO p WHERE " +
           "(:ciudad IS NULL OR LOWER(p.ciudad) LIKE LOWER(CONCAT('%', :ciudad, '%'))) AND " +
           "(:pais IS NULL OR LOWER(p.pais) LIKE LOWER(CONCAT('%', :pais, '%'))) AND " +
           "(:capacidad IS NULL OR p.capacidad >= :capacidad) AND " +
           "(:dormitorios IS NULL OR p.dormitorios >= :dormitorios) AND " +
           "(:banos IS NULL OR p.banos >= :banos) AND " +
           "(p.activo = true)")
    Page<PropiedadVO> busquedaAvanzada(
            @Param("ciudad") String ciudad,
            @Param("pais") String pais,
            @Param("capacidad") Integer capacidad,
            @Param("dormitorios") Integer dormitorios,
            @Param("banos") Integer banos,
            Pageable pageable);
}