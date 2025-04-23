package com.example.demo.propiedad.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.propiedad.model.PropiedadDTO;
import com.example.demo.propiedad.service.PropiedadService;

@RestController
@RequestMapping("/api/propiedades")
public class PropiedadRestController {

    @Autowired
    private PropiedadService propiedadService;
    
    @GetMapping
    public ResponseEntity<Page<PropiedadDTO>> listarPropiedades(
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "9") int size) {
        
        Page<PropiedadDTO> propiedades = propiedadService.obtenerPropiedadesPaginadas(pagina, size);
        return ResponseEntity.ok(propiedades);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PropiedadDTO> obtenerPropiedad(@PathVariable Long id) {
        PropiedadDTO propiedad = propiedadService.obtenerPropiedadPorId(id);
        return ResponseEntity.ok(propiedad);
    }
    
    @GetMapping("/buscar")
    public ResponseEntity<Page<PropiedadDTO>> buscarPropiedades(
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) String pais,
            @RequestParam(required = false) Integer capacidad,
            @RequestParam(required = false) Integer dormitorios,
            @RequestParam(required = false) Integer banos,
            @RequestParam(required = false) Double precioMin,
            @RequestParam(required = false) Double precioMax,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "9") int size) {
        
        Page<PropiedadDTO> propiedades = propiedadService.busquedaAvanzada(
                ciudad, pais, capacidad, dormitorios, banos, precioMin, precioMax, pagina, size);
        
        return ResponseEntity.ok(propiedades);
    }
    
    @GetMapping("/destacadas")
    public ResponseEntity<List<PropiedadDTO>> propiedadesDestacadas() {
        List<PropiedadDTO> propiedades = propiedadService.obtenerPropiedadesDestacadas();
        return ResponseEntity.ok(propiedades);
    }
    
    @GetMapping("/cercanas")
    public ResponseEntity<List<PropiedadDTO>> propiedadesCercanas(
            @RequestParam Double latitud,
            @RequestParam Double longitud,
            @RequestParam(defaultValue = "10") Integer distanciaKm) {
        
        List<PropiedadDTO> propiedades = propiedadService.buscarPropiedadesCercanas(latitud, longitud, distanciaKm);
        return ResponseEntity.ok(propiedades);
    }
    
    @GetMapping("/ciudades-populares")
    public ResponseEntity<List<String>> ciudadesPopulares() {
        List<String> ciudades = propiedadService.obtenerCiudadesPopulares();
        return ResponseEntity.ok(ciudades);
    }
}