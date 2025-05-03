package com.example.demo.admin.control;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.example.demo.email.service.EmailService;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.service.UsuarioService;

@Controller
@RequestMapping("/administrador")
public class AdministradorController {

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private EmailService emailService;

    @GetMapping("/usuarios")
    public String listarUsuarios(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "10") int size,
                                 Model model) {
        List<UsuarioVO> todos = usuarioService.buscarPropietariosEInquilinos();
        int start = page * size;
        int end = Math.min(start + size, todos.size());
        model.addAttribute("usuarios", todos.subList(start, end));
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", (todos.size() + size - 1) / size);
        return "admin/usuarios";
    }

   

    @PostMapping("/usuarios/{username}/email")
    public String sendAdminEmail(@PathVariable String username,
                                 @RequestParam String subject,
                                 @RequestParam String body,
                                 Model model) {
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        try {
            emailService.enviarEmailAdminNotificacion(
                usuario.getEmail(),
                usuario.getNombre(),
                subject,
                body,
                null,
                null
            );
            return "redirect:/administrador/usuarios?emailSent";
        } catch (Exception e) {
            model.addAttribute("error", "Error enviando email: " + e.getMessage());
            return listarUsuarios(0, 10, model);
        }
    }

    @PostMapping("/usuarios/{username}/eliminar")
    public String eliminarUsuario(@PathVariable String username,
                                  @RequestParam String motivo,
                                  Model model) {
        UsuarioVO usuario = usuarioService.buscarPorUsername(username);
        try {
            emailService.enviarEmailCuentaEliminadaPorAdmin(
                usuario.getEmail(),
                usuario.getNombre(),
                motivo
            );
            usuarioService.eliminarUsuarioPorUsername(username);
            return "redirect:/administrador/usuarios?deleted";
        } catch (Exception e) {
            model.addAttribute("error", "No se pudo eliminar / notificar usuario: " + e.getMessage());
            return listarUsuarios(0, 10, model);
        }
    }
}
