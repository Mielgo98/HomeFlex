package com.example.demo.rol.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.rol.model.RolVO;


public interface RolRepository extends JpaRepository<RolVO, Long>{
	 Optional<RolVO> findByNombre(String nombre);
}
