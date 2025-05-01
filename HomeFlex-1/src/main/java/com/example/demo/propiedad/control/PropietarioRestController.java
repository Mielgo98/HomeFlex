package com.example.demo.propiedad.control;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.propiedad.model.PropiedadDTO;
import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.service.PropiedadService;

import java.security.Principal;

@RestController
@RequestMapping("/api/propietario/propiedades")
public class PropietarioRestController {

    private final PropiedadService propiedadService;
    public PropietarioRestController(PropiedadService propiedadService) {
        this.propiedadService = propiedadService;
    }

    @GetMapping
    public Page<PropiedadDTO> listar(
            @RequestParam(required = false) String busqueda,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("fechaCreacion").descending());
        return propiedadService.obtenerPropiedadesPropietarioFiltradas(
                principal.getName(), busqueda, activo, pageable);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        propiedadService.eliminarPropiedad(id);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PropiedadVO> getPropiedad(@PathVariable Long id) {
        PropiedadDTO encontrada = propiedadService.obtenerPropiedadPorId(id);
        PropiedadVO vo = new PropiedadVO(encontrada);
        return ResponseEntity.ok(vo);
    }

  
    @PutMapping("/{id}")
    public ResponseEntity<Void> updatePropiedad(
            @PathVariable Long id,
            @RequestBody PropiedadVO vo
    ) {
        vo.setId(id);
        propiedadService.actualizarPropiedad(id, vo);
        return ResponseEntity.noContent().build();
    }
}
