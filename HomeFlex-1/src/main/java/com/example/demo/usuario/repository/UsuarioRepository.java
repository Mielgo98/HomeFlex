package com.example.demo.usuario.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.usuario.model.UsuarioVO;

@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioVO, Long> {
    Optional<UsuarioVO> findUserEntityByEmail(String email);
    Optional<UsuarioVO> findByUsername(String username);
    Optional<UsuarioVO> findByTokenVerificacion(String token);
}