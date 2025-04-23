// FotoRepository.java
package com.example.demo.foto.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.foto.model.FotoVO;
import com.example.demo.propiedad.model.PropiedadVO;

@Repository
public interface FotoRepository extends JpaRepository<FotoVO, Long> {
    
    List<FotoVO> findByPropiedad(PropiedadVO propiedad);
    
    Optional<FotoVO> findByPropiedadAndPrincipal(PropiedadVO propiedad, boolean principal);
}