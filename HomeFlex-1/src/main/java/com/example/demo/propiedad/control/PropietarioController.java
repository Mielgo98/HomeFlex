package com.example.demo.propiedad.control;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.propiedad.model.PropiedadDTO;
import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.service.PropiedadService;

@Controller
@RequestMapping("/propietario")
public class PropietarioController {

    private final PropiedadService propiedadService;

    public PropietarioController(PropiedadService propiedadService) {
        this.propiedadService = propiedadService;
    }

    /**
     * Muestra la vista Thymeleaf con el listado vacío.
     * El JS se encargará de llamar al REST y poblarla.
     */
    @GetMapping("/propiedades")
    public String verMisPropiedades(Model model, Principal principal) {
        model.addAttribute("username", principal.getName());
        return "propietario/mis-propiedades";
    }

    /**
     * Muestra el formulario de edición, cargando los datos de la propiedad.
     */
    @GetMapping("/propiedades/editar/{id}")
    public String editarForm(@PathVariable Long id, Model model) {
        PropiedadDTO dto = propiedadService.obtenerPropiedadPorId(id);
        PropiedadVO vo = new PropiedadVO(dto);
        model.addAttribute("propiedad", vo);
        return "propietario/propietario-form";
    }

    /**
     * Procesa el envío del formulario de edición.
     */
    @PostMapping("/propiedades/editar/{id}")
    public String editarSubmit(
            @PathVariable Long id,
            @ModelAttribute("propiedad") PropiedadVO vo,
            RedirectAttributes flash
    ) {
        vo.setId(id);
        propiedadService.actualizarPropiedad(id, vo);
        flash.addFlashAttribute("success", "Propiedad actualizada correctamente");
        return "redirect:/propietario/propiedades";
    }

    /**
     * Vista de reservas para el propietario.
     */
    @GetMapping("/reservas")
    public String verReservas() {
        return "propietario/reservas";
    }
}
