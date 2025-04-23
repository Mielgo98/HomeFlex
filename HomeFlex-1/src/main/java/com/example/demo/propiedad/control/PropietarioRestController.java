package com.example.demo.propiedad.control;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.propiedad.model.PropiedadDTO;
import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.service.PropiedadService;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.service.UsuarioService;
import com.example.demo.utils.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/propietario/propiedades")
@PreAuthorize("hasRole('ROLE_PROPIETARIO')")
public class PropietarioRestController {

    @Autowired
    private PropiedadService propiedadService;
    
    @Autowired
    private UsuarioService usuarioService;
    
    @GetMapping("/mis-propiedades")
    public ResponseEntity<Page<PropiedadDTO>> misPropiedades(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String ordenar,
            Principal principal) {
        
        // Obtener el usuario actual
        UsuarioVO propietario = usuarioService.buscarPorUsername(principal.getName());
        
        // Configurar la ordenación
        Sort sort;
        if (ordenar == null || ordenar.equals("fecha_desc")) {
            sort = Sort.by("fechaCreacion").descending();
        } else if (ordenar.equals("fecha_asc")) {
            sort = Sort.by("fechaCreacion").ascending();
        } else if (ordenar.equals("precio_asc")) {
            sort = Sort.by("precioDia").ascending();
        } else if (ordenar.equals("precio_desc")) {
            sort = Sort.by("precioDia").descending();
        } else {
            sort = Sort.by("fechaCreacion").descending();
        }
        
        // Configurar la paginación
        Pageable pageable = PageRequest.of(pagina, size, sort);
        
        // Filtrar según los parámetros
        Boolean activoFilter = null;
        if (estado != null && !estado.isEmpty()) {
            activoFilter = Boolean.parseBoolean(estado);
        }
        
        // Obtener las propiedades del propietario con filtros
        Page<PropiedadDTO> propiedades = propiedadService.obtenerPropiedadesPropietarioFiltradas(
                propietario, busqueda, activoFilter, pageable);
        
        return ResponseEntity.ok(propiedades);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPropiedadParaEditar(
            @PathVariable Long id,
            Principal principal) {
        
        try {
            // Obtener la propiedad
            PropiedadVO propiedad = propiedadService.obtenerPropiedadCompleta(id);
            
            // Verificar que el propietario sea el actual
            if (!propiedad.getPropietario().getUsername().equals(principal.getName())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "No tienes permisos para editar esta propiedad"));
            }
            
            return ResponseEntity.ok(propiedad);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Propiedad no encontrada: " + e.getMessage()));
        }
    }
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> crearPropiedad(
            @RequestPart("propiedad") @Valid PropiedadVO propiedad,
            @RequestPart(value = "fotos", required = false) List<MultipartFile> fotos,
            @RequestPart(value = "fotoPrincipal", required = false) Integer fotoPrincipal,
            Principal principal) {
        
        try {
            // Obtener el usuario actual como propietario
            UsuarioVO propietario = usuarioService.buscarPorUsername(principal.getName());
            
            // Establecer los datos que no vienen del formulario
            propiedad.setPropietario(propietario);
            propiedad.setFechaCreacion(LocalDateTime.now());
            propiedad.setActivo(true);
            
            // Guardar la propiedad
            PropiedadDTO propiedadGuardada = propiedadService.crearPropiedad(propiedad);
            
            // Procesar fotos si hay
            if (fotos != null && !fotos.isEmpty()) {
                propiedadService.procesarFotos(propiedadGuardada.getId(), fotos, fotoPrincipal);
            }
            
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Propiedad creada correctamente", propiedadGuardada));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al crear la propiedad: " + e.getMessage()));
        }
    }
    
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> actualizarPropiedad(
            @PathVariable Long id,
            @RequestPart("propiedad") @Valid PropiedadVO propiedad,
            @RequestPart(value = "fotos", required = false) List<MultipartFile> fotos,
            @RequestPart(value = "fotoPrincipalNueva", required = false) Integer fotoPrincipalNueva,
            @RequestPart(value = "fotoPrincipalExistente", required = false) Long fotoPrincipalExistente,
            @RequestPart(value = "fotosEliminar", required = false) List<Long> fotosEliminar,
            @RequestPart(value = "draft", required = false) Boolean draft,
            Principal principal) {
        
        try {
            // Obtener el usuario actual como propietario
            UsuarioVO propietario = usuarioService.buscarPorUsername(principal.getName());
            
            // Verificar que el propietario sea el correcto
            PropiedadVO propiedadExistente = propiedadService.obtenerPropiedadCompleta(id);
            if (!propiedadExistente.getPropietario().getId().equals(propietario.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "No tienes permisos para editar esta propiedad"));
            }
            
            // Mantener el propietario y fecha de creación originales
            propiedad.setPropietario(propietario);
            propiedad.setFechaCreacion(propiedadExistente.getFechaCreacion());
            
            // Si es un borrador, establecer como inactivo
            if (draft != null && draft) {
                propiedad.setActivo(false);
            }
            
            // Actualizar la propiedad
            PropiedadDTO propiedadActualizada = propiedadService.actualizarPropiedad(id, propiedad);
            
            // Procesar fotos nuevas si hay
            if (fotos != null && !fotos.isEmpty() && fotos.stream().anyMatch(f -> !f.isEmpty())) {
                propiedadService.procesarFotos(propiedadActualizada.getId(), fotos, fotoPrincipalNueva);
            }
            
            // Establecer foto principal si se seleccionó una existente
            if (fotoPrincipalExistente != null) {
                propiedadService.establecerFotoPrincipal(propiedadActualizada.getId(), fotoPrincipalExistente);
            }
            
            // Eliminar fotos seleccionadas
            if (fotosEliminar != null && !fotosEliminar.isEmpty()) {
                propiedadService.eliminarFotos(propiedadActualizada.getId(), fotosEliminar);
            }
            
            return ResponseEntity.ok(new ApiResponse(true, "Propiedad actualizada correctamente", propiedadActualizada));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al actualizar la propiedad: " + e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarPropiedad(
            @PathVariable Long id,
            Principal principal) {
        
        try {
            // Obtener el usuario actual como propietario
            UsuarioVO propietario = usuarioService.buscarPorUsername(principal.getName());
            
            // Verificar que el propietario sea el correcto
            PropiedadVO propiedadExistente = propiedadService.obtenerPropiedadCompleta(id);
            if (!propiedadExistente.getPropietario().getId().equals(propietario.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(false, "No tienes permisos para eliminar esta propiedad"));
            }
            
            // Eliminar la propiedad (desactivación lógica)
            propiedadService.eliminarPropiedad(id);
            
            return ResponseEntity.ok(new ApiResponse(true, "Propiedad eliminada correctamente"));
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al eliminar la propiedad: " + e.getMessage()));
        }
    }
    
    @GetMapping("/estadisticas")
    public ResponseEntity<?> estadisticasPropietario(Principal principal) {
        try {
            UsuarioVO propietario = usuarioService.buscarPorUsername(principal.getName());
            return ResponseEntity.ok(propiedadService.obtenerEstadisticasPropietario(propietario.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponse(false, "Error al obtener estadísticas: " + e.getMessage()));
        }
    }
}