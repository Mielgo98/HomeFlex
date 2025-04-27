package com.example.demo.propiedad.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.foto.model.FotoVO;
import com.example.demo.foto.repository.FotoRepository;
import com.example.demo.propiedad.model.PropiedadDTO;
import com.example.demo.propiedad.model.PropiedadEstadisticasDTO;
import com.example.demo.propiedad.model.PropiedadVO;
import com.example.demo.propiedad.repository.PropiedadRepository;
import com.example.demo.propiedad.event.*;
import com.example.demo.usuario.model.UsuarioVO;

@Service
public class PropiedadService {

	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	
    @Autowired
    private PropiedadRepository propiedadRepository;
    
    @Autowired
    private FotoRepository fotoRepository;
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
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
     * Obtiene una propiedad por su ID como DTO
     */
    public PropiedadDTO obtenerPropiedadPorId(Long id) {
        PropiedadVO propiedad = propiedadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
        
        return new PropiedadDTO(propiedad);
    }
    
    /**
     * Obtiene una propiedad completa por su ID como VO
     */
    public PropiedadVO obtenerPropiedadCompleta(Long id) {
        return propiedadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
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
            Double precioMin,
            Double precioMax,
            int pagina, 
            int tamanoPagina) {
        
        Pageable pageable = PageRequest.of(pagina, tamanoPagina, Sort.by("fechaCreacion").descending());
        
        Page<PropiedadVO> propiedades = propiedadRepository.busquedaAvanzada(
                ciudad, pais, capacidad, dormitorios, banos, pageable);
        
        // Filtro adicional para precios (puede implementarse en la consulta si es necesario)
        if (precioMin != null || precioMax != null) {
            List<PropiedadVO> filtradas = propiedades.getContent().stream()
                .filter(p -> {
                    BigDecimal precio = p.getPrecioDia();
                    boolean cumple = true;
                    
                    if (precioMin != null) {
                        cumple = cumple && precio.doubleValue() >= precioMin;
                    }
                    
                    if (precioMax != null) {
                        cumple = cumple && precio.doubleValue() <= precioMax;
                    }
                    
                    return cumple;
                })
                .collect(Collectors.toList());
            
            return new PageImpl<>(
                filtradas.stream().map(PropiedadDTO::new).collect(Collectors.toList()),
                pageable,
                filtradas.size()
            );
        }
        
        return propiedades.map(PropiedadDTO::new);
    }
    
    /**
     * Crea una nueva propiedad
     */
    @Transactional
    public PropiedadDTO crearPropiedad(PropiedadVO propiedad) {
        // Validar datos básicos
        if (propiedad.getTitulo() == null || propiedad.getDescripcion() == null) {
            throw new RuntimeException("Los campos título y descripción son obligatorios");
        }
        
        // Guardar la propiedad
        PropiedadVO propiedadGuardada = propiedadRepository.save(propiedad);
        applicationEventPublisher.publishEvent(new PropiedadCreatedEvent(propiedadGuardada.getId()));
        return new PropiedadDTO(propiedadGuardada);
    }
    
    /**
     * Crea una nueva propiedad con un propietario específico
     */
    @Transactional
    public PropiedadDTO crearPropiedad(PropiedadVO propiedad, UsuarioVO propietario) {
        propiedad.setPropietario(propietario);
        propiedad.setFechaCreacion(LocalDateTime.now());
        propiedad.setActivo(true);
        
        PropiedadVO propiedadGuardada = propiedadRepository.save(propiedad);
        applicationEventPublisher.publishEvent(new PropiedadCreatedEvent(propiedadGuardada.getId()));
        return new PropiedadDTO(propiedadGuardada);
    }
    
    /**
     * Actualiza una propiedad existente
     */
    @Transactional
    public PropiedadDTO actualizarPropiedad(Long id, PropiedadVO propiedadActualizada) {
        PropiedadVO propiedadExistente = propiedadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
        
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
        propiedadExistente.setActivo(propiedadActualizada.getActivo());
        
        PropiedadVO propiedadGuardada = propiedadRepository.save(propiedadExistente);
        applicationEventPublisher.publishEvent(new PropiedadUpdatedEvent(propiedadGuardada.getId()));
        return new PropiedadDTO(propiedadGuardada);
    }
    
    /**
     * Actualiza una propiedad existente con un propietario específico
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
        applicationEventPublisher.publishEvent(new PropiedadUpdatedEvent(propiedadGuardada.getId()));
        return new PropiedadDTO(propiedadGuardada);
    }
    
    /**
     * Elimina una propiedad (desactivación lógica)
     */
    @Transactional
    public void eliminarPropiedad(Long id) {
        PropiedadVO propiedad = propiedadRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
        
        // Eliminar lógicamente
        propiedad.setActivo(false);
        propiedadRepository.save(propiedad);
        applicationEventPublisher.publishEvent(new PropiedadDeletedEvent(id));

    }
    
    /**
     * Elimina una propiedad (desactivación lógica) verificando el propietario
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
        applicationEventPublisher.publishEvent(new PropiedadDeletedEvent(id));

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
    
    /**
     * Obtiene propiedades del propietario con filtros
     */
    public Page<PropiedadDTO> obtenerPropiedadesPropietarioFiltradas(
            UsuarioVO propietario,
            String busqueda,
            Boolean activo,
            Pageable pageable) {
        
        // Obtenemos todas las propiedades del propietario
        List<PropiedadVO> propiedades = propiedadRepository.findByPropietario(propietario);
        
        // Aplicamos filtros
        List<PropiedadVO> propiedadesFiltradas = propiedades.stream()
            .filter(p -> {
                boolean cumpleBusqueda = true;
                boolean cumpleActivo = true;
                
                // Filtrar por texto de búsqueda
                if (busqueda != null && !busqueda.trim().isEmpty()) {
                    String busquedaLower = busqueda.toLowerCase();
                    cumpleBusqueda = p.getTitulo().toLowerCase().contains(busquedaLower) ||
                                    p.getDescripcion().toLowerCase().contains(busquedaLower) ||
                                    p.getCiudad().toLowerCase().contains(busquedaLower) ||
                                    p.getPais().toLowerCase().contains(busquedaLower);
                }
                
                // Filtrar por estado activo/inactivo
                if (activo != null) {
                    cumpleActivo = p.getActivo() == activo;
                }
                
                return cumpleBusqueda && cumpleActivo;
            })
            .collect(Collectors.toList());
        
        // Aplicar paginación
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), propiedadesFiltradas.size());
        
        // Sublistado para la página actual
        List<PropiedadVO> paginaPropiedades = start < end ? 
                propiedadesFiltradas.subList(start, end) : 
                new ArrayList<>();
        
        // Convertir a DTOs
        List<PropiedadDTO> propiedadesDTO = paginaPropiedades.stream()
                .map(PropiedadDTO::new)
                .collect(Collectors.toList());
        
        return new PageImpl<>(propiedadesDTO, pageable, propiedadesFiltradas.size());
    }
    
    /**
     * Procesa las fotos de una propiedad
     */
    @Transactional
    public void procesarFotos(Long propiedadId, List<MultipartFile> fotos, Integer fotoPrincipalIndex) {
        PropiedadVO propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
        
        // Verificar si ya existe una foto principal
        boolean hayFotoPrincipal = fotoRepository.findByPropiedadAndPrincipal(propiedad, true).isPresent();
        
        // Procesar cada foto
        for (int i = 0; i < fotos.size(); i++) {
            MultipartFile foto = fotos.get(i);
            
            if (foto.isEmpty()) {
                continue; // Saltamos archivos vacíos
            }
            
            try {
                // Generar nombre único para la foto
                String fileName = System.currentTimeMillis() + "_" + foto.getOriginalFilename();
                
                // Crear directorio de subida si no existe
                java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir);
                if (!java.nio.file.Files.exists(uploadPath)) {
                    java.nio.file.Files.createDirectories(uploadPath);
                }
                
                // Guardar el archivo
                java.nio.file.Path filePath = uploadPath.resolve(fileName);
                foto.transferTo(filePath.toFile());
                
                // Crear la entidad FotoVO
                FotoVO fotoVO = new FotoVO();
                fotoVO.setPropiedad(propiedad);
                fotoVO.setUrl("/uploads/" + fileName);
                fotoVO.setDescripcion("Foto de " + propiedad.getTitulo());
                
                // Determinar si es la foto principal
                boolean esPrincipal = fotoPrincipalIndex != null && i == fotoPrincipalIndex;
                if (esPrincipal || (!hayFotoPrincipal && i == 0)) {
                    // Si es la primera foto o la seleccionada como principal y no hay otra principal
                    fotoVO.setPrincipal(true);
                    hayFotoPrincipal = true;
                    
                    // Quitar el estado de principal a otras fotos si es necesario
                    if (i > 0 || esPrincipal) {
                        Optional<FotoVO> anterior = fotoRepository.findByPropiedadAndPrincipal(propiedad, true);
                        anterior.ifPresent(f -> {
                            f.setPrincipal(false);
                            fotoRepository.save(f);
                        });
                    }
                } else {
                    fotoVO.setPrincipal(false);
                }
                
                // Guardar la foto
                fotoRepository.save(fotoVO);
                
            } catch (IOException e) {
                throw new RuntimeException("Error al procesar la foto: " + e.getMessage());
            }
        }
    }
    
    /**
     * Establece una foto como principal
     */
    @Transactional
    public void establecerFotoPrincipal(Long propiedadId, Long fotoId) {
        PropiedadVO propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
        
        FotoVO nuevaPrincipal = fotoRepository.findById(fotoId)
                .orElseThrow(() -> new RuntimeException("Foto no encontrada"));
        
        // Verificar que la foto pertenezca a la propiedad
        if (!nuevaPrincipal.getPropiedad().getId().equals(propiedadId)) {
            throw new RuntimeException("La foto no pertenece a esta propiedad");
        }
        
        // Quitar estado principal a la foto actual
        Optional<FotoVO> actual = fotoRepository.findByPropiedadAndPrincipal(propiedad, true);
        actual.ifPresent(f -> {
            f.setPrincipal(false);
            fotoRepository.save(f);
        });
        
        // Establecer la nueva foto principal
        nuevaPrincipal.setPrincipal(true);
        fotoRepository.save(nuevaPrincipal);
    }
    
    /**
     * Elimina fotos de una propiedad
     */
    @Transactional
    public void eliminarFotos(Long propiedadId, List<Long> fotosIds) {
        PropiedadVO propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));
        
        for (Long fotoId : fotosIds) {
            FotoVO foto = fotoRepository.findById(fotoId)
                    .orElseThrow(() -> new RuntimeException("Foto no encontrada"));
            
            // Verificar que la foto pertenezca a la propiedad
            if (!foto.getPropiedad().getId().equals(propiedadId)) {
                throw new RuntimeException("La foto no pertenece a esta propiedad");
            }
            
            // Si es la foto principal y hay más fotos, establecer otra como principal
            if (foto.isPrincipal()) {
                List<FotoVO> otrasFotos = fotoRepository.findByPropiedad(propiedad);
                otrasFotos.removeIf(f -> f.getId().equals(fotoId) || fotosIds.contains(f.getId()));
                
                if (!otrasFotos.isEmpty()) {
                    FotoVO nuevaPrincipal = otrasFotos.get(0);
                    nuevaPrincipal.setPrincipal(true);
                    fotoRepository.save(nuevaPrincipal);
                }
            }
            
            // Eliminar el archivo físico si es posible
            String filePath = foto.getUrl().replace("/uploads/", "");
            try {
                java.nio.file.Path path = java.nio.file.Paths.get(uploadDir, filePath);
                java.nio.file.Files.deleteIfExists(path);
            } catch (IOException e) {
                // Loguear el error pero continuar con la eliminación de la entidad
                System.err.println("Error al eliminar archivo: " + e.getMessage());
            }
            
            // Eliminar la entidad
            fotoRepository.delete(foto);
        }
    }
    
    /**
     * Obtiene propiedades destacadas
     */
    public List<PropiedadDTO> obtenerPropiedadesDestacadas() {
        Pageable pageable = PageRequest.of(0, 6, Sort.by("fechaCreacion").descending());
        Page<PropiedadVO> propiedades = propiedadRepository.findByActivoTrue(pageable);
        
        return propiedades.getContent().stream()
                .map(PropiedadDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Busca propiedades cercanas según coordenadas
     */
    public List<PropiedadDTO> buscarPropiedadesCercanas(Double latitud, Double longitud, Integer distanciaKm) {
        // Convertir distancia a grados aproximados
        // 1 grado en latitud ≈ 111km, 1 grado en longitud varía con la latitud
        double distanciaLatitud = distanciaKm / 111.0;
        double distanciaLongitud = distanciaKm / (111.0 * Math.cos(Math.toRadians(latitud)));
        
        // Buscar todas las propiedades activas
        List<PropiedadVO> todasPropiedades = propiedadRepository.findByActivoTrue();
        
        // Filtrar por proximidad
        List<PropiedadVO> propiedadesCercanas = todasPropiedades.stream()
            .filter(p -> {
                if (p.getLatitud() == null || p.getLongitud() == null) {
                    return false;
                }
                
                double diffLat = Math.abs(p.getLatitud().doubleValue() - latitud);
                double diffLong = Math.abs(p.getLongitud().doubleValue() - longitud);
                
                return diffLat <= distanciaLatitud && diffLong <= distanciaLongitud;
            })
            .limit(10) // Limitamos a 10 resultados
            .collect(Collectors.toList());
        
        return propiedadesCercanas.stream()
                .map(PropiedadDTO::new)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene las ciudades más populares
     */
    public List<String> obtenerCiudadesPopulares() {
        List<PropiedadVO> propiedades = propiedadRepository.findByActivoTrue();
        
        // Agrupar por ciudad y contar
        Map<String, Long> ciudadesCont = propiedades.stream()
                .collect(Collectors.groupingBy(PropiedadVO::getCiudad, Collectors.counting()));
        
        // Ordenar por cantidad y tomar las top 6
        return ciudadesCont.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(6)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
    
    /**
     * Genera estadísticas para un propietario
     */
    public PropiedadEstadisticasDTO obtenerEstadisticasPropietario(Long propietarioId) {
        PropiedadEstadisticasDTO estadisticas = new PropiedadEstadisticasDTO();
        
        // Obtener propiedades del propietario
        List<PropiedadVO> propiedades = propiedadRepository.findAll().stream()
                .filter(p -> p.getPropietario().getId().equals(propietarioId))
                .collect(Collectors.toList());
        
        // Total de propiedades
        estadisticas.setTotalPropiedades(propiedades.size());
        
        // Propiedades activas
        long activas = propiedades.stream().filter(PropiedadVO::getActivo).count();
        estadisticas.setPropiedadesActivas((int) activas);
        
        // Propiedades inactivas
        estadisticas.setPropiedadesInactivas(propiedades.size() - (int) activas);
        
        // Estadísticas por ciudad
        Map<String, Integer> propiedadesPorCiudad = propiedades.stream()
                .collect(Collectors.groupingBy(PropiedadVO::getCiudad, Collectors.summingInt(p -> 1)));
        estadisticas.setPropiedadesPorCiudad(propiedadesPorCiudad);
        
        // Estadísticas por mes (últimos 12 meses)
        Map<Month, Integer> publicacionesPorMes = new HashMap<>();
        LocalDateTime doceAtras = LocalDateTime.now().minusMonths(12);
        
        propiedades.stream()
            .filter(p -> p.getFechaCreacion() != null && p.getFechaCreacion().isAfter(doceAtras))
            .forEach(p -> {
                Month mes = p.getFechaCreacion().getMonth();
                publicacionesPorMes.put(mes, publicacionesPorMes.getOrDefault(mes, 0) + 1);
            });
        
        Map<String, Integer> publicacionesMes = new HashMap<>();
        for (Month mes : Month.values()) {
            publicacionesMes.put(mes.toString(), publicacionesPorMes.getOrDefault(mes, 0));
        }
        estadisticas.setPublicacionesPorMes(publicacionesMes);
        
        // Cualquier otra estadística que necesites...
        
        return estadisticas;
    }
}