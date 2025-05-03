package com.example.demo.admin.service;

import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.demo.admin.model.LogAccesoVO;
import com.example.demo.admin.repository.LogAccesoRepository;

@Service
public class LogAccesoService {
    
    @Autowired private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private LogAccesoRepository repo;
    
    public List<Map<String,Object>> obtenerAccesosRecientes(int limit) {
        String sql = "SELECT fecha, usuario, accion, ip, url "
                   + "FROM log_accesos ORDER BY fecha DESC LIMIT ?";
        return jdbcTemplate.queryForList(sql, limit);
    }
    
    public Map<String, Long> contarAccionesPorTipo() {
        String sql = "SELECT accion, COUNT(*) AS cantidad FROM log_accesos GROUP BY accion";
        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql);
        Map<String, Long> map = new HashMap<>();
        for (var r : rows) {
            map.put((String)r.get("accion"), ((Number)r.get("cantidad")).longValue());
        }
        return map;
    }
    public Map<String, Long> obtenerAccesosPorHora(int horas) {
        LocalDateTime inicio = LocalDateTime.now().minusHours(horas);
        String sql = """
            SELECT date_part('hour', fecha) AS hora,
                   COUNT(*)                 AS cantidad
              FROM log_accesos
             WHERE fecha >= ?
          GROUP BY date_part('hour', fecha)
        """;

        List<Map<String,Object>> rows = jdbcTemplate.queryForList(sql, inicio);
        Map<String, Long> map = new HashMap<>();
        for (var r : rows) {
            // date_part devuelve un Double: lo convertimos a int
            int hora = ((Number) r.get("hora")).intValue();
            long cnt = ((Number) r.get("cantidad")).longValue();
            map.put(String.valueOf(hora), cnt);
        }
        return map;
    }

    
    public void registrar(LogAccesoVO log) {
        repo.save(log);
    }
}
