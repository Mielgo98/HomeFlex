// PropiedadService.java
package com.example.demo.propiedad.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.propiedad.model.PropiedadDTO;
import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.repository.PropiedadRepository;
import com.example.demo.usuario.model.UsuarioVO;

@Service
public class PropiedadService {

    @Autowired
    private PropiedadRepository propiedadRepository;
    
    /**
     * Obtiene un listado paginado de propiedades activas
     */
    public Page<PropiedadDTO> obtenerPropiedadesPaginadas(int pagina, int tamanoPagina) {
        Pageable pageable = PageRequest.of(pagina, tamanoPagina, Sort.by("fechaCreacion").descending());
        Page<PropiedadVO> propiedades = propiedadRepository.findByActivoTrue(pageable);
        
        return propiedades.map(PropiedadDTO::new);
    }
    
    /**
     * Obtiene todas las propiedades activas (sin paginación)
     */
    public List<PropiedadDTO> obtenerPropiedadesActivas() {
        List<PropiedadVO> propiedades = propiedadRepository.findByActivoTrue();
        
        return propiedades.stream()
                .map(PropiedadDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene una propiedad por su ID
     */
    public PropiedadDTO obtenerPropiedadPorId(Long id) {
        PropiedadVO propiedad = propiedadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
        
        return new PropiedadDTO(propiedad);
    }
    
    /**
     * Busca propiedades por ciudad
     */
    public List<PropiedadDTO> buscarPorCiudad(String ciudad) {
        List<PropiedadVO> propiedades = propiedadRepository.findByCiudadContainingIgnoreCaseAndActivoTrue(ciudad);
        
        return propiedades.stream()
                .map(PropiedadDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Realiza una búsqueda avanzada de propiedades
     */
    public Page<PropiedadDTO> busquedaAvanzada(
            String ciudad, 
            String pais, 
            Integer capacidad, 
            Integer dormitorios, 
            Integer banos, 
            int pagina, 
            int tamanoPagina) {
        
        Pageable pageable = PageRequest.of(pagina, tamanoPagina, Sort.by("fechaCreacion").descending());
        
        Page<PropiedadVO> propiedades = propiedadRepository.busquedaAvanzada(
                ciudad, pais, capacidad, dormitorios, banos, pageable);
        
        return propiedades.map(PropiedadDTO::new);
    }
    
    /**
     * Crea una nueva propiedad
     */
    @Transactional
    public PropiedadDTO crearPropiedad(PropiedadVO propiedad, UsuarioVO propietario) {
        propiedad.setPropietario(propietario);
        propiedad.setFechaCreacion(LocalDateTime.now());
        propiedad.setActivo(true);
        
        PropiedadVO propiedadGuardada = propiedadRepository.save(propiedad);
        
        return new PropiedadDTO(propiedadGuardada);
    }
    
    /**
     * Actualiza una propiedad existente
     */
    @Transactional
    public PropiedadDTO actualizarPropiedad(Long id, PropiedadVO propiedadActualizada, UsuarioVO propietario) {
        PropiedadVO propiedadExistente = propiedadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
        
        // Verificar que el propietario sea el correcto
        if (!propiedadExistente.getPropietario().getId().equals(propietario.getId())) {
            throw new RuntimeException("No tienes permiso para editar esta propiedad");
        }
        
        // Actualizar los campos
        propiedadExistente.setTitulo(propiedadActualizada.getTitulo());
        propiedadExistente.setDescripcion(propiedadActualizada.getDescripcion());
        propiedadExistente.setPrecioDia(propiedadActualizada.getPrecioDia());
        propiedadExistente.setPrecioSemana(propiedadActualizada.getPrecioSemana());
        propiedadExistente.setDireccion(propiedadActualizada.getDireccion());
        propiedadExistente.setCiudad(propiedadActualizada.getCiudad());
        propiedadExistente.setPais(propiedadActualizada.getPais());
        propiedadExistente.setLatitud(propiedadActualizada.getLatitud());
        propiedadExistente.setLongitud(propiedadActualizada.getLongitud());
        propiedadExistente.setCapacidad(propiedadActualizada.getCapacidad());
        propiedadExistente.setDormitorios(propiedadActualizada.getDormitorios());
        propiedadExistente.setBanos(propiedadActualizada.getBanos());
        
        PropiedadVO propiedadGuardada = propiedadRepository.save(propiedadExistente);
        
        return new PropiedadDTO(propiedadGuardada);
    }
    
    /**
     * Elimina una propiedad (desactivación lógica)
     */
    @Transactional
    public void eliminarPropiedad(Long id, UsuarioVO propietario) {
        PropiedadVO propiedad = propiedadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
        
        // Verificar que el propietario sea el correcto
        if (!propiedad.getPropietario().getId().equals(propietario.getId())) {
            throw new RuntimeException("No tienes permiso para eliminar esta propiedad");
        }
        
        // Eliminar lógicamente
        propiedad.setActivo(false);
        propiedadRepository.save(propiedad);
    }
    
    /**
     * Obtiene las propiedades de un propietario
     */
    public List<PropiedadDTO> obtenerPropiedadesPorPropietario(UsuarioVO propietario) {
        List<PropiedadVO> propiedades = propiedadRepository.findByPropietario(propietario);
        
        return propiedades.stream()
                .map(PropiedadDTO::new)
                .collect(Collectors.toList());
    }
}