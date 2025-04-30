package com.example.demo.pago.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.pago.model.PagoVO;

@Repository
public interface PagoRepository extends JpaRepository<PagoVO, Long> {

    Optional<PagoVO> findBySessionId(String sessionId);

    List<PagoVO> findAllByReservaId(Long reservaId);
}
