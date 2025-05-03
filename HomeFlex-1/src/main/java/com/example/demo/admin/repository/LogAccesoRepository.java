package com.example.demo.admin.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.admin.model.LogAccesoVO;

@Repository
public interface LogAccesoRepository extends JpaRepository<LogAccesoVO, Long> {

    /**
     * Obtiene los 10 accesos más recientes, ordenados por fecha descendente.
     */
    List<LogAccesoVO> findTop10ByOrderByFechaDesc();
}
