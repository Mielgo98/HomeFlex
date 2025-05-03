package com.example.demo.valoracion.control;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.demo.utils.ApiResponse;
import com.example.demo.valoracion.model.NuevaValoracionDTO;
import com.example.demo.valoracion.model.RespuestaValoracionDTO;
import com.example.demo.valoracion.model.ValoracionDTO;
import com.example.demo.valoracion.model.ValoracionesEstadisticasDTO;
import com.example.demo.valoracion.service.ValoracionService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/valoraciones")
public class ValoracionRestController {

    @Autowired
    private ValoracionService valoracionService;
    
    // ── Obtener todas las valoraciones de una propiedad (paginadas) ──
    @GetMapping("/propiedad/{propiedadId}")
    public ResponseEntity<?> obtenerValoracionesPropiedad(
            @PathVariable Long propiedadId,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(pagina, size, Sort.by("fechaCreacion").descending());
            Page<ValoracionDTO> valoraciones =
                valoracionService.obtenerValoracionesPorPropiedadPaginadas(propiedadId, pageable);
            return ResponseEntity.ok(valoraciones);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al obtener valoraciones: " + e.getMessage()));
        }
    }

    // ── **Nuevo**: Obtener una valoración por su ID ────────────────
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerValoracionPorId(
            @PathVariable Long id,
            Principal principal) {
        try {
            ValoracionDTO dto = valoracionService.obtenerValoracionPorId(id);
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ApiResponse(false, "Valoración no encontrada: " + e.getMessage()));
        }
    }
    
    // ── Estadísticas de valoraciones ──────────────────────────────
    @GetMapping("/propiedad/{propiedadId}/estadisticas")
    public ResponseEntity<?> obtenerEstadisticasPropiedad(@PathVariable Long propiedadId) {
        try {
            ValoracionesEstadisticasDTO estadisticas =
                valoracionService.calcularEstadisticasPropiedad(propiedadId);
            return ResponseEntity.ok(estadisticas);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al obtener estadísticas: " + e.getMessage()));
        }
    }

    // ── CRUD de valoraciones y respuesta/proceso ──────────────────
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> crearValoracion(
            @Valid @RequestBody NuevaValoracionDTO dto,
            Principal principal) {
        try {
            ValoracionDTO valoracion = valoracionService.crearValoracion(dto, principal.getName());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponse(true, "Valoración creada correctamente", valoracion));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al crear la valoración: " + e.getMessage()));
        }
    }

    @PostMapping("/propiedad/{propiedadId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> crearParaPropiedad(
            @PathVariable Long propiedadId,
            @Valid @RequestBody NuevaValoracionDTO dto,
            Principal principal) {
        dto.setPropiedadId(propiedadId);
        return crearValoracion(dto, principal);
    }

    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> actualizarValoracion(
            @PathVariable Long id,
            @Valid @RequestBody NuevaValoracionDTO dto,
            Principal principal) {
        try {
            ValoracionDTO valoracion = valoracionService.actualizarValoracion(id, dto, principal.getName());
            return ResponseEntity.ok(new ApiResponse(true, "Valoración actualizada correctamente", valoracion));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al actualizar la valoración: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> eliminarValoracion(
            @PathVariable Long id,
            Principal principal) {
        try {
            valoracionService.eliminarValoracion(id, principal.getName());
            return ResponseEntity.ok(new ApiResponse(true, "Valoración eliminada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al eliminar la valoración: " + e.getMessage()));
        }
    }

    @PostMapping("/responder")
    @PreAuthorize("hasRole('ROLE_PROPIETARIO')")
    public ResponseEntity<?> responderValoracion(
            @Valid @RequestBody RespuestaValoracionDTO dto,
            Principal principal) {
        try {
            ValoracionDTO valoracion = valoracionService.responderValoracion(dto, principal.getName());
            return ResponseEntity.ok(new ApiResponse(true, "Respuesta enviada correctamente", valoracion));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al responder la valoración: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/aprobar")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> aprobarValoracion(
            @PathVariable Long id,
            Principal principal) {
        try {
            ValoracionDTO valoracion = valoracionService.aprobarValoracion(id, principal.getName());
            return ResponseEntity.ok(new ApiResponse(true, "Valoración aprobada correctamente", valoracion));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al aprobar la valoración: " + e.getMessage()));
        }
    }

    @PostMapping("/{id}/rechazar")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> rechazarValoracion(
            @PathVariable Long id,
            Principal principal) {
        try {
            valoracionService.rechazarValoracion(id, principal.getName());
            return ResponseEntity.ok(new ApiResponse(true, "Valoración rechazada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al rechazar la valoración: " + e.getMessage()));
        }
    }

    @GetMapping("/mis-valoraciones")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> obtenerMisValoraciones(Principal principal) {
        try {
            return ResponseEntity.ok(valoracionService.obtenerValoracionesPorUsuario(principal.getName()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al obtener tus valoraciones: " + e.getMessage()));
        }
    }

    @GetMapping("/mis-propiedades")
    @PreAuthorize("hasRole('ROLE_PROPIETARIO')")
    public ResponseEntity<?> obtenerValoracionesMisPropiedades(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        try {
            Pageable pageable = PageRequest.of(pagina, size, Sort.by("fechaCreacion").descending());
            Page<ValoracionDTO> valoraciones = valoracionService
                .obtenerValoracionesPorPropietarioPaginadas(principal.getName(), pageable);
            return ResponseEntity.ok(valoraciones);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al obtener valoraciones: " + e.getMessage()));
        }
    }

    @GetMapping("/pendientes-respuesta")
    @PreAuthorize("hasRole('ROLE_PROPIETARIO')")
    public ResponseEntity<?> obtenerPendientesRespuesta(Principal principal) {
        try {
            return ResponseEntity.ok(valoracionService.obtenerValoracionesPendientesRespuesta(principal.getName()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al obtener valoraciones pendientes: " + e.getMessage()));
        }
    }

    @GetMapping("/pendientes")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> obtenerValoraciolesPendientes() {
        try {
            return ResponseEntity.ok(valoracionService.obtenerValoracionesPendientes());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new ApiResponse(false, "Error al obtener valoraciones pendientes: " + e.getMessage()));
        }
    }

}
