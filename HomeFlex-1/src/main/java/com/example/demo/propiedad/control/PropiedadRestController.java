package com.example.demo.propiedad.control;

import java.awt.print.Pageable;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.propiedad.model.PropiedadDTO;
import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.service.PropiedadService;
import com.example.demo.reserva.model.ReservaDTO;
import com.example.demo.reserva.service.ReservaService;
import com.example.demo.rol.model.RolVO;
import com.example.demo.rol.repository.RolRepository;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.repository.UsuarioRepository;
import com.example.demo.usuario.service.UsuarioService;
import com.example.demo.valoracion.model.ValoracionDTO;
import com.example.demo.valoracion.model.ValoracionesEstadisticasDTO;
import com.example.demo.valoracion.service.ValoracionService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/propiedades")
public class PropiedadRestController {

    @Autowired private PropiedadService propiedadService;
    @Autowired private UsuarioService  usuarioService;
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private ValoracionService valoracionService;
    
    @Autowired
    private ReservaService reservaService;
    
    @Autowired
    private RolRepository rolRepository;
    
    /** fragmento modal (solo se usa si se carga de forma asíncrona) */
    @GetMapping("/nueva")
    public String formNuevaPropiedad(Model model) {
        model.addAttribute("propiedad", new PropiedadVO());
        return "propiedad/formulario :: formNuevaPropiedad";
    }

    /**
     * Procesa el formulario modal.  
     *  – Crea la propiedad  
     *  – Asigna ROLE_PROPIETARIO si el usuario solo era INQUILINO  
     *  – Guarda las fotos en media/propiedades  
     *  – Redirige a /index con SweetAlert
     * @throws IOException 
     */
    @PreAuthorize("hasRole('ROLE_INQUILINO') or hasRole('ROLE_PROPIETARIO')")
    @PostMapping("/propiedades")
    public String crearPropiedad(
            @Valid                     @ModelAttribute("propiedad") PropiedadVO propiedad,
                                       BindingResult result,
                                       @RequestParam("ficheros") List<MultipartFile> ficheros,
                                       @RequestParam(value="fotoPrincipalIndex", required=false)
                                       Integer fotoPrincipalIndex,
                                       Principal principal,
                                       RedirectAttributes redirect,
                                       Model model) throws IOException {

        if (result.hasErrors()) {
        	 model.addAttribute("showModal", true);
        	 System.out.println("Vaya errores");
        	 
        	  System.out.println("⛔️ VALIDATION ERRORS:");
        	    result.getFieldErrors().forEach(fe ->
        	        System.out.printf("  • %s %s → %s%n",
        	                fe.getField(), fe.getRejectedValue(), fe.getDefaultMessage()));

        	    result.getGlobalErrors().forEach(ge ->
        	        System.out.printf("  • %s → %s%n",
        	                ge.getObjectName(), ge.getDefaultMessage()));
        	 
             return "index";
        }

        UsuarioVO usuario = usuarioRepository.findByUsername(principal.getName())
                            .orElseThrow();

        if (!usuario.tieneRol("PROPIETARIO")) {
            RolVO rolPropietario = rolRepository.findByNombre("PROPIETARIO")
                .orElseThrow(() -> new IllegalStateException("El rol PROPIETARIO no existe"));

            usuario.addRol(rolPropietario);       
            usuarioRepository.save(usuario);
        }

     System.out.println("Propiedad a punto de crearse");
        PropiedadDTO dto = propiedadService.crearPropiedad(propiedad, usuario);
        propiedadService.procesarFotos(dto.getId(), ficheros, fotoPrincipalIndex);
        System.out.println("Propiedad a creada");
        /* ---------- SweetAlert en index ---------- */
        redirect.addFlashAttribute("mensaje", "¡Propiedad publicada con éxito!");
        return "redirect:/index";
    }


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
     * Búsqueda avanzada con/-sin disponibilidad y paginación.
     *
     * @param ciudad        filtro por ciudad (contiene, case-insensitive)
     * @param pais          filtro por país  (contiene, case-insensitive)
     * @param capacidad     nº mínimo de huéspedes
     * @param dormitorios   nº mínimo de dormitorios
     * @param banos         nº mínimo de baños
     * @param precioMin     precio mínimo por día  (EUR)
     * @param precioMax     precio máximo por día  (EUR)
     * @param fechaInicio   fecha de entrada (opcional)
     * @param fechaFin      fecha de salida  (opcional)
     * @param pagina        nº de página (0-based)
     * @param size          tamaño de página
     */
    @GetMapping
    public String listarPropiedades(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false)
            @DateTimeFormat(iso = ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false)
            @DateTimeFormat(iso = ISO.DATE) LocalDate fechaFin,
            @RequestParam(required = false) String ordenar,
            Model model) {

        Sort sort;
        switch (ordenar == null ? "" : ordenar) {
            case "precio_asc"  -> sort = Sort.by("precioDia").ascending();
            case "precio_desc" -> sort = Sort.by("precioDia").descending();
            case "fecha_asc"   -> sort = Sort.by("fechaCreacion").ascending();
            default            -> sort = Sort.by("fechaCreacion").descending();
        }
        PageRequest pageable = PageRequest.of(pagina, size, sort);

        Page<PropiedadDTO> propiedades = propiedadService.busquedaAvanzada(
                ciudad,          // ciudad
                null,            // pais  (aún no se filtra, se pasa null)
                null, null, null,// capacidad, dormitorios, banos
                null, null,      // precioMin, precioMax
                fechaInicio,     // fechaInicio
                fechaFin,        // fechaFin
                pagina,          // página
                size);           // tamaño

        /* 3. Datos para la vista */
        model.addAttribute("propiedades", propiedades);
        model.addAttribute("paginaActual", pagina);
        model.addAttribute("totalPaginas", propiedades.getTotalPages());
        model.addAttribute("pageSize", propiedades.getSize());
        model.addAttribute("ordenar", ordenar);
        model.addAttribute("ciudad", ciudad);
        model.addAttribute("fechaInicio", fechaInicio);
        model.addAttribute("fechaFin", fechaFin);
        model.addAttribute("ciudadesPopulares",
                           propiedadService.obtenerCiudadesPopulares());

        return "propiedad/listado";
    }
}
