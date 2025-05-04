package com.example.demo.propiedad.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collections;
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
import com.example.demo.rol.model.RolVO;
import com.example.demo.rol.repository.RolRepository;
import com.example.demo.propiedad.event.*;
import com.example.demo.usuario.model.UsuarioVO;
import com.example.demo.usuario.repository.UsuarioRepository;

@Service
public class PropiedadService {

	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	
    @Autowired
    private PropiedadRepository propiedadRepository;
    
    @Autowired
    private FotoRepository fotoRepository;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired private RolRepository rolRepository;
    
    @Value("${homeflex.upload-dir}")
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
     * Búsqueda avanzada incluyendo disponibilidad en un rango de fechas.
     * Todas las fechas son opcionales; si no se reciben, no se filtra por disponibilidad.
     */
    public Page<PropiedadDTO> busquedaAvanzada(
            String ciudad, String pais,
            Integer capacidad, Integer dormitorios, Integer banos,
            Double precioMin, Double precioMax,
            LocalDate fechaInicio, LocalDate fechaFin,
            int pagina, int size) {

        Pageable pageable = PageRequest.of(
                pagina, size, Sort.by("fechaCreacion").descending());

        BigDecimal min = precioMin != null ? BigDecimal.valueOf(precioMin) : null;
        BigDecimal max = precioMax != null ? BigDecimal.valueOf(precioMax) : null;

        Page<PropiedadVO> page;

        if (fechaInicio != null && fechaFin != null) {
            page = propiedadRepository.buscarConDisponibilidad(
                    ciudad, pais, capacidad, dormitorios, banos,
                    min, max, fechaInicio, fechaFin, pageable);
        } else {
            page = propiedadRepository.buscarSinDisponibilidad(
                    ciudad, pais, capacidad, dormitorios, banos,
                    min, max, pageable);
        }

        return page.map(PropiedadDTO::new);
    }


    
    
    /**
     * Crea una nueva propiedad
     */
    @Transactional
    public PropiedadDTO crearPropiedad(
            PropiedadVO propiedad,                  // datos de la vivienda
            UsuarioVO propietario,                  // usuario que publica
            List<MultipartFile> fotos,              // imágenes subidas
            Integer fotoPrincipalIndex)             // índice de la principal
            throws IOException {

    	// Verifica si el usuario ya tiene ese rol
    	RolVO rolPropietario = rolRepository.findByNombre("ROLE_PROPIETARIO")
    	 .orElseThrow(() -> new RuntimeException("Rol no encontrado en BD"));
        if (!propietario.getRoles().contains(rolPropietario)) {
            propietario.getRoles().add(rolPropietario);
            usuarioRepository.save(propietario); 
        }
        /* -------- 1. datos básicos -------- */
        propiedad.setPropietario(propietario);
        propiedad.setFechaCreacion(LocalDateTime.now());
        propiedad.setActivo(true);

        PropiedadVO propiedadGuardada = propiedadRepository.save(propiedad);

        /* -------- 2. subida de imágenes -------- */
        if (fotos != null && !fotos.isEmpty()) {
            // crea la carpeta  media/propiedades/{id}/
            Path baseDir = Paths.get(uploadDir, propiedadGuardada.getId().toString());
            Files.createDirectories(baseDir);

            boolean hayPrincipal = false;
            for (int i = 0; i < fotos.size(); i++) {
                MultipartFile file = fotos.get(i);
                if (file.isEmpty()) continue;

                String nombre = System.currentTimeMillis() + "_"
                              + file.getOriginalFilename()
                                    .replaceAll("[^a-zA-Z0-9.-]", "_");
                Path destino = baseDir.resolve(nombre);
                file.transferTo(destino);

                FotoVO foto = new FotoVO();
                foto.setPropiedad(propiedadGuardada);
                foto.setUrl("/" + uploadDir + "/"
                            + propiedadGuardada.getId() + "/" + nombre);

                // principal: la indicada o la primera si no había otra
                boolean esPrincipal = (fotoPrincipalIndex != null && i == fotoPrincipalIndex)
                                    || (!hayPrincipal && i == 0);
                foto.setPrincipal(esPrincipal);
                hayPrincipal |= esPrincipal;

                fotoRepository.save(foto);
            }
        }

        /* -------- 3. ascender a PROPIETARIO si aún no lo es -------- */
        boolean yaEsPropietario = propietario.getRoles().stream()
             .anyMatch(r -> r.getNombre().equals("PROPIETARIO"));

        /* -------- 4. publicar el evento + devolver DTO -------- */
        applicationEventPublisher.publishEvent(
                new PropiedadCreatedEvent(propiedadGuardada.getId()));

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
        propiedadRepository.delete(propiedad);
        System.out.println("Eliminada");
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
     * Obtiene propiedades del propietario con filtros de búsqueda, estado y paginación
     * @param username Nombre de usuario del propietario
     * @param busqueda Texto para filtrar propiedades
     * @param activo Estado de activación de propiedades
     * @param pageable Información de paginación
     * @return Página de DTOs de propiedades
     */
    public Page<PropiedadDTO> obtenerPropiedadesPropietarioFiltradas(
            String username,
            String busqueda,
            Boolean activo,
            Pageable pageable) {

        // 1. Buscar el usuario por username
        UsuarioVO propietario = usuarioRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado con username: " + username));
            
        // 2. Obtiene todas las propiedades del propietario según los filtros
        Page<PropiedadVO> propiedades;
        
        if (busqueda != null && !busqueda.trim().isEmpty()) {
            if (activo != null) {
                // Con búsqueda y filtro de activo
                propiedades = propiedadRepository.findByPropietarioAndBusquedaAndActivo(
                    propietario, busqueda.toLowerCase(), activo, pageable);
            } else {
                // Solo con búsqueda
                propiedades = propiedadRepository.findByPropietarioAndBusqueda(
                    propietario, busqueda.toLowerCase(), pageable);
            }
        } else {
            if (activo != null) {
                // Solo con filtro de activo
                propiedades = propiedadRepository.findByPropietarioAndActivo(
                    propietario, activo, pageable);
            } else {
                // Sin filtros
                propiedades = propiedadRepository.findByPropietario(propietario, pageable);
            }
        }

        // 3. Mapea los resultados a DTOs
        return propiedades.map(PropiedadDTO::new);
    }
    
    @Transactional
    public void procesarFotos(Long propiedadId,
                              List<MultipartFile> fotos,
                              Integer fotoPrincipalIndex) throws IOException {

        PropiedadVO propiedad = propiedadRepository.findById(propiedadId)
                .orElseThrow(() -> new RuntimeException("Propiedad no encontrada"));

        if (fotos == null || fotos.isEmpty()) return;

        // ─────────────────────────────────────────────────────────────
        Path uploadPath = Paths.get(uploadDir, "propiedades")   
                                .toAbsolutePath()
                                .normalize()
                                .resolve(propiedadId.toString()); 
        Files.createDirectories(uploadPath);
        // ─────────────────────────────────────────────────────────────

        boolean hayPrincipal = fotoRepository
                .findByPropiedadAndPrincipal(propiedad, true).isPresent();

        for (int i = 0; i < fotos.size(); i++) {

            MultipartFile foto = fotos.get(i);
            if (foto.isEmpty()) continue;

            String original = Optional.ofNullable(foto.getOriginalFilename())
                                      .orElse("imagen.jpg");

            String fileName = System.currentTimeMillis() + "_" +
                              original.replaceAll("[^a-zA-Z0-9.-]", "_");

            // ★ Escribimos la imagen
            Path destino = uploadPath.resolve(fileName);
            foto.transferTo(destino);

            // ★ Persistimos metadatos
            FotoVO fotoVO = new FotoVO();
            fotoVO.setPropiedad(propiedad);

            //   URL pública = handler /media/ + ruta relativa al uploadDir
            fotoVO.setUrl("/media/propiedades/" + propiedadId + "/" + fileName);
            fotoVO.setDescripcion("Foto de " + propiedad.getTitulo());

            boolean esPrincipal = (fotoPrincipalIndex != null && i == fotoPrincipalIndex)
                                  || (!hayPrincipal && i == 0);

            if (esPrincipal) {
                // quitamos la anterior principal (si existe)
                fotoRepository.findByPropiedadAndPrincipal(propiedad, true)
                              .ifPresent(f -> { f.setPrincipal(false); fotoRepository.save(f); });
                fotoVO.setPrincipal(true);
                hayPrincipal = true;
            } else {
                fotoVO.setPrincipal(false);
            }

            fotoRepository.save(fotoVO);
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
        
        // Si ya es principal, no hay que hacer nada
        if (nuevaPrincipal.isPrincipal()) {
            return;
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
        
        // Verificar si hay fotos para eliminar
        if (fotosIds == null || fotosIds.isEmpty()) {
            return;
        }
        
        // Obtener todas las fotos de la propiedad
        List<FotoVO> todasLasFotos = fotoRepository.findByPropiedad(propiedad);
        boolean eliminarPrincipal = false;
        List<FotoVO> fotosAEliminar = new ArrayList<>();
        
        // Identificar las fotos a eliminar
        for (Long fotoId : fotosIds) {
            Optional<FotoVO> fotoOpt = todasLasFotos.stream()
                    .filter(f -> f.getId().equals(fotoId))
                    .findFirst();
            
            if (fotoOpt.isPresent()) {
                FotoVO foto = fotoOpt.get();
                fotosAEliminar.add(foto);
                
                // Verificar si vamos a eliminar la principal
                if (foto.isPrincipal()) {
                    eliminarPrincipal = true;
                }
            }
        }
        
        // Si eliminamos la principal y quedan más fotos, asignar una nueva principal
        if (eliminarPrincipal && todasLasFotos.size() > fotosAEliminar.size()) {
            // Encontrar la primera foto que no vamos a eliminar
            Optional<FotoVO> nuevaPrincipal = todasLasFotos.stream()
                    .filter(f -> !fotosAEliminar.contains(f))
                    .findFirst();
            
            nuevaPrincipal.ifPresent(f -> {
                f.setPrincipal(true);
                fotoRepository.save(f);
            });
        }
        
        // Eliminar cada foto
        for (FotoVO foto : fotosAEliminar) {
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
        try {
            // Utilizar la consulta optimizada que carga las fotos en la misma consulta
            Pageable pageable = PageRequest.of(0, 6, Sort.by("fechaCreacion").descending());
            Page<PropiedadVO> propiedadesPage = propiedadRepository.findByActivoTrueWithFotos(pageable);
            
            return propiedadesPage.getContent().stream()
                .map(PropiedadDTO::new)
                .collect(Collectors.toList());
            
        } catch (Exception e) {
            // En caso de error, intentar con el método alternativo
            System.err.println("Error al obtener propiedades destacadas: " + e.getMessage());
            
            // Plan B: obtener propiedades sin las fotos y establecer un placeholder
            List<PropiedadVO> propiedades = propiedadRepository.findByActivoTrue();
            List<PropiedadDTO> propiedadesDTOs = new ArrayList<>();
            
            for (PropiedadVO propiedad : propiedades.stream()
                                              .limit(6)
                                              .collect(Collectors.toList())) {
                PropiedadDTO dto = new PropiedadDTO();
                dto.setId(propiedad.getId());
                dto.setTitulo(propiedad.getTitulo());
                dto.setDescripcion(propiedad.getDescripcion());
                dto.setPrecioDia(propiedad.getPrecioDia());
                dto.setCiudad(propiedad.getCiudad());
                dto.setPais(propiedad.getPais());
                dto.setCapacidad(propiedad.getCapacidad());
                dto.setDormitorios(propiedad.getDormitorios());
                dto.setBanos(propiedad.getBanos());
                dto.setFotoPrincipal("/images/property-placeholder.jpg"); // Placeholder por defecto
                
                propiedadesDTOs.add(dto);
            }
            
            return propiedadesDTOs;
        }
    }
    
    
    /**
     * Busca propiedades cercanas según coordenadas
     */
    public List<PropiedadDTO> buscarPropiedadesCercanas(Double latitud, Double longitud, Integer distanciaKm) {
        if (latitud == null || longitud == null) {
            throw new RuntimeException("Las coordenadas son obligatorias");
        }
        
        if (distanciaKm == null || distanciaKm <= 0) {
            distanciaKm = 10; // Valor por defecto
        }
        
        double distanciaLatitud = distanciaKm / 111.0;
        double distanciaLongitud = distanciaKm / (111.0 * Math.cos(Math.toRadians(latitud)));
        
        // Buscar propiedades cercanas utilizando una consulta optimizada
        List<PropiedadVO> propiedadesCercanas = propiedadRepository.findPropiedadesCercanas(
                latitud, longitud, distanciaLatitud, distanciaLongitud);
        
        return propiedadesCercanas.stream()
                .limit(10) // Limitamos a 10 resultados
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
        
        // Obtener propiedades del propietario usando el repository
        List<PropiedadVO> propiedades = propiedadRepository.findByPropietarioId(propietarioId);
        
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
        
        return estadisticas;
    }
}