package com.example.demo.propiedad.control;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.propiedad.model.PropiedadDTO;
import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.service.PropiedadService;
import com.example.demo.reserva.model.ReservaDTO;
import com.example.demo.reserva.service.ReservaService;
import com.example.demo.valoracion.model.ValoracionDTO;
import com.example.demo.valoracion.model.ValoracionesEstadisticasDTO;
import com.example.demo.valoracion.service.ValoracionService;

@Controller
@RequestMapping("/propiedades")
public class PropiedadRestController {

    @Autowired
    private PropiedadService propiedadService;
    
    @Autowired
    private ValoracionService valoracionService;
    
    @Autowired
    private ReservaService reservaService;
    
    /**
     * Muestra el listado de propiedades con paginación y filtros
     */
    @GetMapping
    public String listarPropiedades(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) String ordenar,
            Model model) {
        
        // Configurar la ordenación
        Sort sort;
        if ("precio_asc".equals(ordenar)) {
            sort = Sort.by("precioDia").ascending();
        } else if ("precio_desc".equals(ordenar)) {
            sort = Sort.by("precioDia").descending();
        } else if ("fecha_asc".equals(ordenar)) {
            sort = Sort.by("fechaCreacion").ascending();
        } else {
            // Por defecto, ordenar por fecha de creación descendente
            sort = Sort.by("fechaCreacion").descending();
        }
        
        // Construir el PageRequest
        PageRequest pageable = PageRequest.of(pagina, size, sort);
        
        // Obtener las propiedades (con o sin filtro de ciudad)
        Page<PropiedadDTO> propiedades;
        if (ciudad != null && !ciudad.isEmpty()) {
            propiedades = propiedadService
                .busquedaAvanzada(ciudad, null, null, null, null, null, null, pagina, size);
        } else {
            propiedades = propiedadService.obtenerPropiedadesPaginadas(pagina, size);
        }
        
        // Atributos para la vista
        model.addAttribute("propiedades", propiedades);
        model.addAttribute("paginaActual", pagina);
        model.addAttribute("totalPaginas", propiedades.getTotalPages());
        model.addAttribute("pageSize", propiedades.getSize());        // ← Nuevo atributo
        model.addAttribute("ordenar", ordenar);
        model.addAttribute("ciudad", ciudad);
        model.addAttribute("ciudadesPopulares", propiedadService.obtenerCiudadesPopulares());
        
        return "propiedad/listado";
    }
    
    /**
     * Muestra el detalle de una propiedad con información completa
     */
    @GetMapping("/{id}")
    public String verPropiedad(@PathVariable Long id, Model model, Principal principal) {
        try {
            // Obtener todos los datos de la propiedad
            PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(id);
            model.addAttribute("propiedad", propiedad);
            
            // Obtener valoraciones de la propiedad
            List<ValoracionDTO> valoraciones = valoracionService.obtenerValoracionesPorPropiedad(id);
            model.addAttribute("valoraciones", valoraciones);
            
            // Obtener estadísticas de valoraciones si hay alguna
            if (!valoraciones.isEmpty()) {
                ValoracionesEstadisticasDTO estadisticas = valoracionService.calcularEstadisticasPropiedad(id);
                model.addAttribute("estadisticasValoraciones", estadisticas);
            }
            
            // Verificar si el usuario actual ya ha reservado esta propiedad
            boolean puedeValorar = false;
            if (principal != null) {
                String username = principal.getName();
                List<ReservaDTO> reservasUsuario = reservaService.obtenerReservasUsuario(username);
                puedeValorar = reservasUsuario.stream()
                    .anyMatch(r -> r.getPropiedadId().equals(id));
                model.addAttribute("puedeValorar", puedeValorar);
            }
            
            // Obtener propiedades similares (misma ciudad, capacidad similar)
            List<PropiedadDTO> propiedadesSimilares = propiedadService.buscarPropiedadesCercanas(
                propiedad.getLatitud().doubleValue(), 
                propiedad.getLongitud().doubleValue(), 
                10 // Radio en km
            );
            // Filtrar la propiedad actual de las similares
            propiedadesSimilares.removeIf(p -> p.getId().equals(id));
            // Limitar a máximo 4 propiedades similares
            if (propiedadesSimilares.size() > 4) {
                propiedadesSimilares = propiedadesSimilares.subList(0, 4);
            }
            model.addAttribute("propiedadesSimilares", propiedadesSimilares);
            
            return "propiedad/detalle";
        } catch (Exception e) {
            model.addAttribute("error", "Error al cargar la propiedad: " + e.getMessage());
            return "error";
        }
    }
    
    /**
     * Muestra el formulario de búsqueda avanzada de propiedades
     */
    @GetMapping("/buscar")
    public String formularioBusqueda(Model model) {
        // Obtener ciudades populares para sugerencias de autocompletado
        List<String> ciudadesPopulares = propiedadService.obtenerCiudadesPopulares();
        model.addAttribute("ciudadesPopulares", ciudadesPopulares);
        
        return "propiedad/busqueda";
    }
    
    /**
     * Procesa la búsqueda avanzada de propiedades
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
        
        // Realizar búsqueda con los criterios proporcionados
        Page<PropiedadDTO> propiedades = propiedadService.busquedaAvanzada(
                ciudad, pais, capacidad, dormitorios, banos, precioMin, precioMax, pagina, size);
        
        model.addAttribute("propiedades", propiedades);
        model.addAttribute("paginaActual", pagina);
        model.addAttribute("totalPaginas", propiedades.getTotalPages());
        model.addAttribute("ciudad", ciudad);
        model.addAttribute("pais", pais);
        model.addAttribute("capacidad", capacidad);
        model.addAttribute("dormitorios", dormitorios);
        model.addAttribute("banos", banos);
        model.addAttribute("precioMin", precioMin);
        model.addAttribute("precioMax", precioMax);
        
        return "propiedad/resultados";
    }
}
