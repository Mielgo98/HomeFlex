package com.example.demo.propietario.controller;

import java.security.Principal;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.service.PropiedadService;
import com.example.demo.reserva.model.EstadoReserva;
import com.example.demo.reserva.model.ReservaDTO;
import com.example.demo.reserva.service.ReservaService;
import com.example.demo.usuario.model.UsuarioVO;

@Controller
@RequestMapping("/propietario")
public class PropietarioController {

    private final PropiedadService propiedadService;
    private final ReservaService reservaService;

    @Autowired
    public PropietarioController(PropiedadService propiedadService,
                                 ReservaService reservaService) {
        this.propiedadService = propiedadService;
        this.reservaService = reservaService;
    }

    //
    // === PROPIEDADES ===
    //

    @GetMapping("/propiedades")
    public String listarPropiedades(
            @RequestParam Optional<String> busqueda,
            @RequestParam Optional<Boolean> activo,
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<Integer> size,
            Principal principal,
            Model model) {

        int pagina = page.orElse(0);
        int tam    = size.orElse(10);

        PageRequest pageable = PageRequest.of(pagina, tam);

        UsuarioVO usuarioFiltro = new UsuarioVO();
        usuarioFiltro.setUsername(principal.getName());

        var pageDto = propiedadService
            .obtenerPropiedadesPropietarioFiltradas(
                usuarioFiltro,
                busqueda.orElse(""),
                activo.orElse(true),
                pageable
            );

        model.addAttribute("propiedades",   pageDto.getContent());
        model.addAttribute("totalPaginas",  pageDto.getTotalPages());
        model.addAttribute("paginaActual",  pagina);
        model.addAttribute("busqueda",      busqueda.orElse(""));
        model.addAttribute("activo",        activo.orElse(true));

        return "propietario/propiedades";
    }

    @GetMapping("/propiedades/nueva")
    public String nuevaPropiedad(Model model) {
        model.addAttribute("propiedad", new PropiedadVO());
        return "propietario/propiedad-form";
    }

    @PostMapping("/propiedades")
    public String guardarPropiedad(@ModelAttribute PropiedadVO propiedad,
                                   Principal principal,
                                   RedirectAttributes ra) {
        UsuarioVO usuario = new UsuarioVO();
        usuario.setUsername(principal.getName());
        propiedadService.crearPropiedad(propiedad, usuario);
        ra.addFlashAttribute("success", "Propiedad creada correctamente");
        return "redirect:/propietario/propiedades";
    }

    @GetMapping("/propiedades/{id}/editar")
    public String editarPropiedad(@PathVariable Long id,
                                  Model model,
                                  RedirectAttributes ra) {
        try {
            var dto = propiedadService.obtenerPropiedadPorId(id);
            model.addAttribute("propiedad", new PropiedadVO(dto));
            return "propietario/propiedad-form";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Propiedad no encontrada");
            return "redirect:/propietario/propiedades";
        }
    }

    @PostMapping("/propiedades/{id}")
    public String actualizarPropiedad(@PathVariable Long id,
                                      @ModelAttribute PropiedadVO propiedad,
                                      Principal principal,
                                      RedirectAttributes ra) {
        try {
            UsuarioVO usuario = new UsuarioVO();
            usuario.setUsername(principal.getName());
            propiedadService.actualizarPropiedad(id, propiedad, usuario);
            ra.addFlashAttribute("success", "Propiedad actualizada");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/propietario/propiedades";
    }

    @PostMapping("/propiedades/{id}/eliminar")
    public String eliminarPropiedad(@PathVariable Long id,
                                    Principal principal,
                                    RedirectAttributes ra) {
        try {
            UsuarioVO usuario = new UsuarioVO();
            usuario.setUsername(principal.getName());
            propiedadService.eliminarPropiedad(id, usuario);
            ra.addFlashAttribute("success", "Propiedad eliminada");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/propietario/propiedades";
    }

    //
    // === RESERVAS ===
    //

    @GetMapping("/reservas")
    public String listarReservas(
            @RequestParam Optional<EstadoReserva> estado,
            @RequestParam Optional<String> busqueda,
            @RequestParam Optional<Integer> page,
            @RequestParam Optional<Integer> size,
            Principal principal,
            Model model) {

        int pagina = page.orElse(0);
        int tam    = size.orElse(10);

        PageRequest pageable = PageRequest.of(pagina, tam);

        var pageDto = reservaService
            .filtrarReservasPropietario(
                principal.getName(),
                estado.orElse(null),
                busqueda.orElse(""),
                pageable
            );

        model.addAttribute("reservas",      pageDto.getContent());
        model.addAttribute("totalPaginas",  pageDto.getTotalPages());
        model.addAttribute("paginaActual",  pagina);
        model.addAttribute("estado",        estado.orElse(null));
        model.addAttribute("busqueda",      busqueda.orElse(""));

        return "propietario/reservas";
    }

    @GetMapping("/reservas/{id}")
    public String detalleReserva(@PathVariable Long id,
                                 Principal principal,
                                 Model model,
                                 RedirectAttributes ra) {
        try {
            ReservaDTO dto = reservaService.obtenerReservaPorId(id);
            model.addAttribute("reserva", dto);
            return "propietario/reserva-detalle";
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Reserva no encontrada");
            return "redirect:/propietario/reservas";
        }
    }

    @PostMapping("/reservas/{id}/aprobar")
    public String aprobar(@PathVariable Long id,
                          Principal principal,
                          RedirectAttributes ra) {
        try {
            reservaService.aprobarSolicitud(id, principal.getName());
            ra.addFlashAttribute("success", "Solicitud aprobada");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/propietario/reservas";
    }

    @PostMapping("/reservas/{id}/rechazar")
    public String rechazar(@PathVariable Long id,
                           @RequestParam Optional<String> motivo,
                           Principal principal,
                           RedirectAttributes ra) {
        try {
            reservaService.rechazarSolicitud(id, principal.getName(), motivo.orElse(""));
            ra.addFlashAttribute("success", "Solicitud rechazada");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/propietario/reservas";
    }

    @PostMapping("/reservas/{id}/confirmar")
    public String confirmar(@PathVariable Long id,
                            Principal principal,
                            RedirectAttributes ra) {
        try {
            reservaService.confirmarReserva(id, principal.getName());
            ra.addFlashAttribute("success", "Reserva confirmada");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/propietario/reservas";
    }

    @PostMapping("/reservas/{id}/cancelar")
    public String cancelar(@PathVariable Long id,
                           @RequestParam Optional<String> motivo,
                           Principal principal,
                           RedirectAttributes ra) {
        try {
            reservaService.cancelarReserva(id, principal.getName(), motivo.orElse(""));
            ra.addFlashAttribute("success", "Reserva cancelada");
        } catch (Exception e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/propietario/reservas";
    }

}