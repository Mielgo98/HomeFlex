package com.example.demo.propiedad.control;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.propiedad.model.PropiedadDTO;
import com.example.demo.propiedad.service.PropiedadService;

import java.security.Principal;

@RestController
public class PropietarioRestController {

    private final PropiedadService propiedadService;

    @Autowired
    public PropietarioRestController(PropiedadService propiedadService) {
        this.propiedadService = propiedadService;
    }

    @GetMapping("/api/propietario/propiedades")
    public ResponseEntity<Page<PropiedadDTO>> listarPropiedadesRest(
            Principal principal,
            @RequestParam(name = "busqueda", required = false, defaultValue = "") String busqueda,
            @RequestParam(name = "activo",    required = false) Boolean activo,
            Pageable pageable
    ) {
        // Validar que hay un usuario autenticado
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        
        // Delegamos al servicio, usando el email del propietario
        Page<PropiedadDTO> resultado = propiedadService
            .obtenerPropiedadesPropietarioFiltradas(
                principal.getName(),
                busqueda,
                activo,
                pageable
            );
        
        return ResponseEntity.ok(resultado);
    }
}