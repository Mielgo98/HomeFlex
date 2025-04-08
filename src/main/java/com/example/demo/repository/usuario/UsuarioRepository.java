package com.example.demo.repository.usuario;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.model.usuario.UsuarioVO;


@Repository
public interface UsuarioRepository extends JpaRepository<UsuarioVO, Long> {
	public Optional<UsuarioVO> findUserEntityByEmail(String email);
	
	 Optional<UsuarioVO> findByTokenVerificacion(String token);
}
