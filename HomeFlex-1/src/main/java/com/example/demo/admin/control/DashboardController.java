package com.example.demo.admin.control;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.demo.admin.service.LogAccesoService;
import com.example.demo.usuario.service.UsuarioService;

@Controller
@RequestMapping("/admin/dashboard")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class DashboardController {
    @Autowired private LogAccesoService logAccesoService;
    @Autowired private UsuarioService usuarioService;
    
    @GetMapping
    public String dashboard(Model model) {
        Map<String, Long> usuariosPorRol = usuarioService.contarUsuariosPorRol();
        List<Map<String,Object>> accesosRecientes = logAccesoService.obtenerAccesosRecientes(10);
        Map<String, Long> accionesPorTipo = logAccesoService.contarAccionesPorTipo();
        Map<String, Long> accesosUlt24h = logAccesoService.obtenerAccesosPorHora(24);
        
        model.addAttribute("usuariosPorRol", usuariosPorRol);
        model.addAttribute("accesosRecientes", accesosRecientes);
        model.addAttribute("accionesPorTipo", accionesPorTipo);
        
        String[] horas = new String[24];
        Long[] conteos = new Long[24];
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
        for (int i = 0; i < 24; i++) {
            LocalDateTime h = LocalDateTime.now().minusHours(23 - i);
            horas[i] = h.format(fmt);
            conteos[i] = accesosUlt24h.getOrDefault(String.valueOf(h.getHour()), 0L);
        }
        model.addAttribute("horasAcceso", horas);
        model.addAttribute("conteosAcceso", conteos);
        
        return "super-admin/dashboard";
    }
}
