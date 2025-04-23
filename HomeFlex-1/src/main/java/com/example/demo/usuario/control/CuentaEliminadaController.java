package com.example.demo.usuario.control;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CuentaEliminadaController {
    
    /**
     * Muestra la página de confirmación después de eliminar la cuenta
     */
    @GetMapping("/cuenta-eliminada")
    public String mostrarPaginaConfirmacion() {
        return "usuario/cuenta-eliminada";
    }
}