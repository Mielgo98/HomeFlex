package com.example.demo.admin.control;

import com.example.demo.admin.service.LogAccesoService;
import com.example.demo.usuario.model.RegistroDTO;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.service.UsuarioService;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/superadmin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class AdminController {

	@Autowired
    private  UsuarioService usuarioService;

	 @Autowired 
	 private LogAccesoService logAccesoService;
    public AdminController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/usuarios")
    public String listar(Model model) {
        List<UsuarioVO> admins = usuarioService.obtenerAdministradores();
        model.addAttribute("administradores", admins);
        return "super-admin/administradores";
    }

    @PostMapping("/usuarios/crear")
    @ResponseBody
    public ResponseEntity<?> crear(
        @Valid @RequestBody RegistroDTO dto,
        BindingResult br
    ) {
        if (br.hasErrors()) {
            Map<String, String> errors = new HashMap<>();
            br.getFieldErrors().forEach(fe ->
                errors.put(fe.getField(), fe.getDefaultMessage())
            );
            return ResponseEntity.badRequest().body(errors);
        }
        usuarioService.registrarAdministrador(dto);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/usuarios/{id}/eliminar")
    public String eliminar(
        @PathVariable Long id,
        RedirectAttributes attrs
    ) {
        usuarioService.eliminarUsuario(id);
        attrs.addFlashAttribute("success", "Administrador eliminado.");
        return "redirect:/superadmin/usuarios";
    }
    
    @GetMapping("/dashboard")
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
