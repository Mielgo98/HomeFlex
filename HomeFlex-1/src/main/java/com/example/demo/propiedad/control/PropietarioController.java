package com.example.demo.propiedad.control;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.demo.propiedad.model.PropiedadDTO;
import com.example.demo.propiedad.service.PropiedadService;
import com.example.demo.usuario.service.UsuarioService;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Optional;

@Controller
public class PropietarioController {

    private final PropiedadService propiedadService;
    private final UsuarioService usuarioService;

    @Autowired
    public PropietarioController(PropiedadService propiedadService, UsuarioService usuarioService) {
        this.propiedadService = propiedadService;
        this.usuarioService = usuarioService;
    }

    @GetMapping("/propietario/propiedades")
    public String listarPropiedades(
            Principal principal,
            Model model,
            Pageable pageable,
            HttpServletRequest request
    ) {
        // Sacamos los parámetros de filtrado (si vienen)
        Optional<String> busqueda = Optional.ofNullable(request.getParameter("busqueda"));
        Optional<Boolean> activo = Optional.ofNullable(request.getParameter("activo"))
                                          .map(Boolean::valueOf);

        // Llamamos al servicio pasando el username del propietario
        Page<PropiedadDTO> page = propiedadService
            .obtenerPropiedadesPropietarioFiltradas(
                principal.getName(), // Pasamos el username
                busqueda.orElse(""),
                activo.orElse(null),
                pageable
            );

        // IMPORTANTE: Pasamos el objeto Page completo, no solo el contenido
        model.addAttribute("propiedades", page);
        model.addAttribute("busqueda", busqueda.orElse(""));
        model.addAttribute("activo", activo.orElse(null));
        return "propietario/mis-propiedades";
    }
}