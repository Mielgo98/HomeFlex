package com.example.demo.repository.rol;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.rol.RolVO;

public interface RolRepository extends JpaRepository<RolVO, Long>{
	 Optional<RolVO> findByNombre(String nombre);
}
