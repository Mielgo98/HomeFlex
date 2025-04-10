package com.example.demo.usuario.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.usuario.model.UsuarioVO;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioVO, Long> {
    Optional<UsuarioVO> findUserEntityByEmail(String email);
    Optional<UsuarioVO> findByUsername(String username);
    Optional<UsuarioVO> findByTokenVerificacion(String token);
    
    /**
     * Busca usuarios por estado de verificacion y que tengan un token de verificacion
     * @param verificado  estado de verificacion el cual sera falso para buscar aquellos usuarios que no hayan verificado su cuenta
     * @return retorna una lista de usuarios que coinciden con su atributo verificado a false y que su token de verificacion no sea nulo 
     */
    	List<UsuarioVO>findByVerificadoAndTokenVerificacionIsNotNull(boolean verificado);
}