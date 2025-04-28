package com.example.demo.propiedad.repository;

import java.math.BigDecimal;
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
    
    // Buscar propiedades de un propietario paginadas
    Page<PropiedadVO> findByPropietario(UsuarioVO propietario, Pageable pageable);
    
    // Buscar propiedades por estado de activación
    List<PropiedadVO> findByPropietarioAndActivo(UsuarioVO propietario, Boolean activo);
    
    // Buscar propiedades de un propietario por estado de activación paginadas
    Page<PropiedadVO> findByPropietarioAndActivo(UsuarioVO propietario, Boolean activo, Pageable pageable);
    
    // Buscar propiedades por ciudad
    List<PropiedadVO> findByCiudadContainingIgnoreCaseAndActivoTrue(String ciudad);
    
    // Buscar propiedades por país
    List<PropiedadVO> findByPaisContainingIgnoreCaseAndActivoTrue(String pais);
    
    // Buscar propiedades con capacidad igual o superior
    List<PropiedadVO> findByCapacidadGreaterThanEqualAndActivoTrue(Integer capacidad);
    
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
    
    // Búsqueda avanzada con rango de precios
    @Query("SELECT p FROM PropiedadVO p WHERE " +
           "(:ciudad IS NULL OR LOWER(p.ciudad) LIKE LOWER(CONCAT('%', :ciudad, '%'))) AND " +
           "(:pais IS NULL OR LOWER(p.pais) LIKE LOWER(CONCAT('%', :pais, '%'))) AND " +
           "(:capacidad IS NULL OR p.capacidad >= :capacidad) AND " +
           "(:dormitorios IS NULL OR p.dormitorios >= :dormitorios) AND " +
           "(:banos IS NULL OR p.banos >= :banos) AND " +
           "(:precioMin IS NULL OR p.precioDia >= :precioMin) AND " +
           "(:precioMax IS NULL OR p.precioDia <= :precioMax) AND " +
           "(p.activo = true)")
    Page<PropiedadVO> busquedaAvanzadaConPrecios(
            @Param("ciudad") String ciudad,
            @Param("pais") String pais,
            @Param("capacidad") Integer capacidad,
            @Param("dormitorios") Integer dormitorios,
            @Param("banos") Integer banos,
            @Param("precioMin") BigDecimal precioMin,
            @Param("precioMax") BigDecimal precioMax,
            Pageable pageable);
    
    // Buscar por texto en título o descripción
    @Query("SELECT p FROM PropiedadVO p WHERE " +
           "(LOWER(p.titulo) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "LOWER(p.descripcion) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "LOWER(p.ciudad) LIKE LOWER(CONCAT('%', :texto, '%')) OR " +
           "LOWER(p.pais) LIKE LOWER(CONCAT('%', :texto, '%'))) AND " +
           "(p.activo = true)")
    Page<PropiedadVO> buscarPorTexto(@Param("texto") String texto, Pageable pageable);
    
    // Buscar propiedades cercanas a un punto (aproximación simple por rangos)
    @Query("SELECT p FROM PropiedadVO p WHERE " +
           "p.latitud BETWEEN :latMin AND :latMax AND " +
           "p.longitud BETWEEN :longMin AND :longMax AND " +
           "p.activo = true")
    List<PropiedadVO> buscarPropiedadesCercanas(
            @Param("latMin") BigDecimal latMin,
            @Param("latMax") BigDecimal latMax,
            @Param("longMin") BigDecimal longMin,
            @Param("longMax") BigDecimal longMax);
    
    // Estadísticas: contar propiedades por propietario
    @Query("SELECT COUNT(p) FROM PropiedadVO p WHERE p.propietario.id = :propietarioId")
    Long contarPropiedadesPorPropietario(@Param("propietarioId") Long propietarioId);
    
    // Estadísticas: contar propiedades activas por propietario
    @Query("SELECT COUNT(p) FROM PropiedadVO p WHERE p.propietario.id = :propietarioId AND p.activo = true")
    Long contarPropiedadesActivasPorPropietario(@Param("propietarioId") Long propietarioId);
    
    // Estadísticas: agrupar propiedades por ciudad para un propietario
    @Query("SELECT p.ciudad, COUNT(p) FROM PropiedadVO p WHERE p.propietario.id = :propietarioId GROUP BY p.ciudad")
    List<Object[]> agruparPropiedadesPorCiudad(@Param("propietarioId") Long propietarioId);
    
    long countByActivoTrue();
}