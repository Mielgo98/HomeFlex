package com.example.demo.propiedad.control;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.propiedad.model.PropiedadDTO;
import com.example.demo.propiedad.model.PropiedadEstadisticasDTO;
import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.service.PropiedadService;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.service.UsuarioService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/propiedades")
public class PropiedadRestController {

    @Autowired
    private PropiedadService propiedadService;
    
    @Autowired
    private UsuarioService usuarioService;
    
    /**
     * Muestra el listado de propiedades con paginación
     */
    @GetMapping
    public String listarPropiedades(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "9") int size,
            Model model) {
        
        Page<PropiedadDTO> propiedades = propiedadService.obtenerPropiedadesPaginadas(pagina, size);
        
        model.addAttribute("propiedades", propiedades);
        model.addAttribute("paginaActual", pagina);
        model.addAttribute("totalPaginas", propiedades.getTotalPages());
        
        return "propiedad/listado";
    }
    
    /**
     * Muestra el detalle de una propiedad
     */
    @GetMapping("/{id}")
    public String verPropiedad(@PathVariable Long id, Model model) {
        try {
            PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(id);
            
            model.addAttribute("propiedad", propiedad);
            
            return "propiedad/detalle";
        } catch (RuntimeException e) {
            model.addAttribute("error", "Propiedad no encontrada: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Muestra el formulario para buscar propiedades
     */
    @GetMapping("/buscar")
    public String formularioBusqueda(Model model) {
        // Obtener ciudades populares para sugerencias
        List<String> ciudadesPopulares = propiedadService.obtenerCiudadesPopulares();
        model.addAttribute("ciudadesPopulares", ciudadesPopulares);
        
        return "propiedad/busqueda";
    }
    
    /**
     * Procesa la búsqueda de propiedades
     */
    @GetMapping("/resultados")
    public String resultadosBusqueda(
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) String pais,
            @RequestParam(required = false) Integer capacidad,
            @RequestParam(required = false) Integer dormitorios,
            @RequestParam(required = false) Integer banos,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "9") int size,
            Model model) {
        
        Page<PropiedadDTO> propiedades = propiedadService.busquedaAvanzada(
                ciudad, pais, capacidad, dormitorios, banos, precioMin, precioMax, pagina, size);
        
        model.addAttribute("propiedades", propiedades);
        model.addAttribute("paginaActual", pagina);
        model.addAttribute("totalPaginas", propiedades.getTotalPages());
        
        // Mantener los parámetros de búsqueda para la paginación
        model.addAttribute("ciudad", ciudad);
        model.addAttribute("pais", pais);
        model.addAttribute("capacidad", capacidad);
        model.addAttribute("dormitorios", dormitorios);
        model.addAttribute("banos", banos);
        model.addAttribute("precioMin", precioMin);
        model.addAttribute("precioMax", precioMax);
        
        return "propiedad/resultados";
    }
    
    /**
     * Muestra el listado de propiedades del propietario actual
     */
    @GetMapping("/mis-propiedades")
    public String misPropiedades(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estado,
            Model model,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        // Obtener usuario actual
        UsuarioVO usuario = usuarioService.buscarPorUsername(principal.getName());
        
        // Configurar paginación
        PageRequest pageable = PageRequest.of(pagina, size);
        
        // Convertir string estado a boolean
        Boolean activoFilter = null;
        if (estado != null && !estado.isEmpty()) {
            activoFilter = Boolean.parseBoolean(estado);
        }
        
        // Obtener propiedades del propietario
        Page<PropiedadDTO> propiedades = propiedadService.obtenerPropiedadesPropietarioFiltradas(
                usuario, busqueda, activoFilter, pageable);
        
        model.addAttribute("propiedades", propiedades);
        model.addAttribute("paginaActual", pagina);
        model.addAttribute("totalPaginas", propiedades.getTotalPages());
        
        // Mantener los parámetros de filtrado
        model.addAttribute("busqueda", busqueda);
        model.addAttribute("estado", estado);
        
        return "propiedad/mis-propiedades";
    }
    
    /**
     * Muestra el formulario para crear una nueva propiedad
     */
    @GetMapping("/nueva")
    public String formularioNuevaPropiedad(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("propiedad", new PropiedadVO());
        model.addAttribute("esNueva", true);
        
        return "propiedad/formulario";
    }
    
    /**
     * Procesa la creación de una nueva propiedad
     */
    @PostMapping("/nueva")
    public String procesarNuevaPropiedad(
            @Valid @ModelAttribute("propiedad") PropiedadVO propiedad,
            BindingResult bindingResult,
            @RequestParam(value = "fotos", required = false) List<MultipartFile> fotos,
            @RequestParam(value = "fotoPrincipal", required = false) Integer fotoPrincipal,
            Model model,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("esNueva", true);
            return "propiedad/formulario";
        }
        
        try {
            // Obtener el usuario actual
            UsuarioVO propietario = usuarioService.buscarPorUsername(principal.getName());
            
            // Establecer propietario y fecha
            propiedad.setPropietario(propietario);
            
            // Guardar la propiedad
            PropiedadDTO propiedadGuardada = propiedadService.crearPropiedad(propiedad);
            
            // Procesar fotos si las hay
            if (fotos != null && !fotos.isEmpty()) {
                propiedadService.procesarFotos(propiedadGuardada.getId(), fotos, fotoPrincipal);
            }
            
            redirectAttributes.addFlashAttribute("mensaje", "Propiedad creada con éxito");
            return "redirect:/propiedades/mis-propiedades";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error al crear la propiedad: " + e.getMessage());
            model.addAttribute("esNueva", true);
            return "propiedad/formulario";
        }
    }
    
    /**
     * Muestra el formulario para editar una propiedad
     */
    @GetMapping("/editar/{id}")
    public String formularioEditarPropiedad(@PathVariable Long id, Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(id);
            
            // Verificar que el propietario sea el usuario actual
            if (!propiedad.getPropietario().getUsername().equals(principal.getName())) {
                model.addAttribute("error", "No tienes permiso para editar esta propiedad");
                return "error";
            }
            
            model.addAttribute("propiedad", propiedad);
            model.addAttribute("esNueva", false);
            
            return "propiedad/formulario";
            
        } catch (RuntimeException e) {
            model.addAttribute("error", "Propiedad no encontrada: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Procesa la edición de una propiedad
     */
    @PostMapping("/editar/{id}")
    public String procesarEditarPropiedad(
            @PathVariable Long id,
            @Valid @ModelAttribute("propiedad") PropiedadVO propiedad,
            BindingResult bindingResult,
            @RequestParam(value = "fotos", required = false) List<MultipartFile> fotos,
            @RequestParam(value = "fotoPrincipalNueva", required = false) Integer fotoPrincipalNueva,
            @RequestParam(value = "fotoPrincipalExistente", required = false) Long fotoPrincipalExistente,
            @RequestParam(value = "fotosEliminar", required = false) List<Long> fotosEliminar,
            Model model,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("esNueva", false);
            return "propiedad/formulario";
        }
        
        try {
            // Obtener el usuario actual
            UsuarioVO propietario = usuarioService.buscarPorUsername(principal.getName());
            
            // Verificar que sea el propietario
            PropiedadVO propiedadExistente = propiedadService.obtenerPropiedadCompleta(id);
            if (!propiedadExistente.getPropietario().getUsername().equals(principal.getName())) {
                model.addAttribute("error", "No tienes permiso para editar esta propiedad");
                return "error";
            }
            
            // Actualizar la propiedad
            PropiedadDTO propiedadActualizada = propiedadService.actualizarPropiedad(id, propiedad);
            
            // Procesar nuevas fotos si las hay
            if (fotos != null && !fotos.isEmpty() && fotos.stream().anyMatch(f -> !f.isEmpty())) {
                propiedadService.procesarFotos(propiedadActualizada.getId(), fotos, fotoPrincipalNueva);
            }
            
            // Establecer foto principal existente
            if (fotoPrincipalExistente != null) {
                propiedadService.establecerFotoPrincipal(propiedadActualizada.getId(), fotoPrincipalExistente);
            }
            
            // Eliminar fotos seleccionadas
            if (fotosEliminar != null && !fotosEliminar.isEmpty()) {
                propiedadService.eliminarFotos(propiedadActualizada.getId(), fotosEliminar);
            }
            
            redirectAttributes.addFlashAttribute("mensaje", "Propiedad actualizada con éxito");
            return "redirect:/propiedades/mis-propiedades";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error al actualizar la propiedad: " + e.getMessage());
            model.addAttribute("esNueva", false);
            return "propiedad/formulario";
        }
    }
    
    /**
     * Elimina una propiedad (desactivación lógica)
     */
    @GetMapping("/eliminar/{id}")
    public String eliminarPropiedad(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            Principal principal) {
        
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            // Obtener el usuario actual
            UsuarioVO propietario = usuarioService.buscarPorUsername(principal.getName());
            
            // Verificar que sea el propietario
            PropiedadVO propiedadExistente = propiedadService.obtenerPropiedadCompleta(id);
            if (!propiedadExistente.getPropietario().getUsername().equals(principal.getName())) {
                redirectAttributes.addFlashAttribute("error", "No tienes permiso para eliminar esta propiedad");
                return "redirect:/propiedades/mis-propiedades";
            }
            
            // Eliminar la propiedad
            propiedadService.eliminarPropiedad(id);
            
            redirectAttributes.addFlashAttribute("mensaje", "Propiedad eliminada con éxito");
            return "redirect:/propiedades/mis-propiedades";
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al eliminar la propiedad: " + e.getMessage());
            return "redirect:/propiedades/mis-propiedades";
        }
    }
    
    /**
     * Muestra estadísticas de las propiedades del propietario
     */
    @GetMapping("/estadisticas")
    public String estadisticasPropiedades(Model model, Principal principal) {
        if (principal == null) {
            return "redirect:/login";
        }
        
        try {
            // Obtener el usuario actual
            UsuarioVO propietario = usuarioService.buscarPorUsername(principal.getName());
            
            // Obtener estadísticas
            PropiedadEstadisticasDTO estadisticas = propiedadService.obtenerEstadisticasPropietario(propietario.getId());
            
            model.addAttribute("estadisticas", estadisticas);
            
            return "propiedad/estadisticas";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error al obtener estadísticas: " + e.getMessage());
            return "error";
        }
    }
}