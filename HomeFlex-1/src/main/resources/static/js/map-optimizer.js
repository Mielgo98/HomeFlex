/**
 * Módulo de optimización de mapas para HomeFlex
 * 
 * Este script mejora la experiencia de carga de mapas en la aplicación mediante:
 * 1. Carga diferida (lazy loading) de mapas solo cuando son visibles
 * 2. Optimización de rendimiento para dispositivos móviles
 * 3. Gestión de fallos y estados de carga
 * 4. Soporte para coordenadas inválidas o faltantes
 */

// Configuración global para mapas
const MAP_CONFIG = {
  defaultLocation: [40.416775, -3.703790], // Madrid como ubicación predeterminada
  defaultZoom: 15,
  minZoom: 3,
  maxZoom: 18,
  tileProvider: 'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
  attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
  mapOptions: {
    scrollWheelZoom: false,
    updateWhenIdle: true,
    preferCanvas: true,
    renderer: L.canvas && L.canvas() // Solo si está disponible
  }
};

// Clase principal para gestión de mapas
class MapManager {
  constructor(mapElementId, coordsConfig = {}) {
    this.mapElement = document.getElementById(mapElementId);
    this.map = null;
    this.marker = null;
    this.isInitialized = false;
    this.coordsConfig = coordsConfig;
    this.loadingIndicator = null;
    
    // Si no hay elemento de mapa, salir
    if (!this.mapElement) {
      console.warn('Elemento de mapa no encontrado:', mapElementId);
      return;
    }
    
    // Crear indicador de carga
    this.createLoadingIndicator();
    
    // Configurar observador para carga diferida
    this.setupObserver();
  }
  
  /**
   * Crea un indicador visual durante la carga del mapa
   */
  createLoadingIndicator() {
    this.loadingIndicator = document.createElement('div');
    this.loadingIndicator.className = 'map-loading-indicator';
    this.loadingIndicator.innerHTML = `
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Cargando mapa...</span>
      </div>
      <p class="mt-2">Cargando mapa...</p>
    `;
    
    // Estilos para el indicador
    Object.assign(this.loadingIndicator.style, {
      position: 'absolute',
      top: '0',
      left: '0',
      width: '100%',
      height: '100%',
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'center',
      justifyContent: 'center',
      backgroundColor: '#f8f9fa',
      zIndex: '400',
      borderRadius: 'inherit'
    });
    
    this.mapElement.style.position = 'relative';
    this.mapElement.appendChild(this.loadingIndicator);
  }
  
  /**
   * Configura un Intersection Observer para inicializar el mapa 
   * solo cuando sea visible en la pantalla
   */
  setupObserver() {
    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) {
        this.initializeMap();
        observer.unobserve(this.mapElement);
      }
    }, {
      rootMargin: '200px', // Cargar mapa cuando esté a 200px de ser visible
      threshold: 0.1
    });
    
    observer.observe(this.mapElement);
  }
  
  /**
   * Inicializa el mapa de Leaflet con la configuración proporcionada
   */
  initializeMap() {
    if (this.isInitialized) return;
    
    try {
      // Obtener coordenadas
      const coords = this.getCoordinates();
      
      // Crear mapa
      this.map = L.map(this.mapElement.id, {
        center: coords,
        zoom: MAP_CONFIG.defaultZoom,
        ...MAP_CONFIG.mapOptions
      });
      
      // Añadir capa de tiles
      L.tileLayer(MAP_CONFIG.tileProvider, {
        attribution: MAP_CONFIG.attribution,
        maxZoom: MAP_CONFIG.maxZoom,
        minZoom: MAP_CONFIG.minZoom
      }).addTo(this.map);
      
      // Crear URL de Google Maps
      const googleMapsUrl = `https://www.google.com/maps/search/?api=1&query=${coords[0]},${coords[1]}`;
      
      // Crear contenido del popup
      const popupContent = `
        <div class="map-popup">
          <strong>Ver ubicación</strong>
          <p class="mb-2">Pulsa para ver esta ubicación en Google Maps</p>
          <a href="${googleMapsUrl}" target="_blank" class="btn btn-sm btn-primary">
            <i class="bi bi-google"></i> Abrir en Google Maps
          </a>
        </div>
      `;
      
      // Añadir marcador con popup ya configurado
      this.marker = L.marker(coords)
        .bindPopup(popupContent)
        .addTo(this.map);
      
      // Añadir evento de clic directamente al marcador (forma alternativa)
      this.marker.on('click', function() {
        this.openPopup();
      });
      
      // Añadir también botón de control para abrir Google Maps directamente
      const googleMapsControl = L.control({ position: 'topright' });
      googleMapsControl.onAdd = () => {
        const div = L.DomUtil.create('div', 'leaflet-bar leaflet-control');
        div.innerHTML = `
          <a href="${googleMapsUrl}" target="_blank" title="Abrir en Google Maps" class="google-maps-button">
            <img src="https://maps.gstatic.com/mapfiles/api-3/images/google_gray.svg" alt="Google" style="height: 16px; margin: 6px;">
          </a>
        `;
        return div;
      };
      googleMapsControl.addTo(this.map);
      
      // Configurar interacción con el mapa
      this.setupMapInteraction();
      
      // Marcar como inicializado
      this.isInitialized = true;
      
      // Quitar indicador de carga
      if (this.loadingIndicator) {
        this.loadingIndicator.remove();
      }
      
      // Forzar recálculo de tamaño para mapas que puedan estar ocultos inicialmente
      setTimeout(() => {
        if (this.map) this.map.invalidateSize();
      }, 300);
      
      console.log('Mapa inicializado correctamente');
    } catch (error) {
      this.handleMapError(error);
    }
  }
  
  /**
   * Obtiene las coordenadas para el mapa, con manejo de valores inválidos
   */
  getCoordinates() {
    let lat, lon;
    
    // Intentar obtener coordenadas de elementos o atributos
    if (this.coordsConfig.latElement && this.coordsConfig.lonElement) {
      const latElement = document.getElementById(this.coordsConfig.latElement);
      const lonElement = document.getElementById(this.coordsConfig.lonElement);
      
      if (latElement && lonElement) {
        lat = parseFloat(latElement.value || latElement.textContent);
        lon = parseFloat(lonElement.value || lonElement.textContent);
      }
    } 
    // Si se proporcionaron directamente en la configuración
    else if (this.coordsConfig.lat !== undefined && this.coordsConfig.lon !== undefined) {
      lat = parseFloat(this.coordsConfig.lat);
      lon = parseFloat(this.coordsConfig.lon);
    }
    
    // Validar que sean números válidos
    if (isNaN(lat) || isNaN(lon) || !isFinite(lat) || !isFinite(lon)) {
      console.warn('Coordenadas inválidas, usando ubicación predeterminada');
      return MAP_CONFIG.defaultLocation;
    }
    
    // Validar que estén en rangos correctos
    if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
      console.warn('Coordenadas fuera de rango, usando ubicación predeterminada');
      return MAP_CONFIG.defaultLocation;
    }
    
    return [lat, lon];
  }
  
  /**
   * Configura la interacción del usuario con el mapa
   */
  setupMapInteraction() {
    // Habilitar scroll wheel zoom solo al hacer clic en el mapa
    this.map.on('click', () => {
      if (!this.map.scrollWheelZoom.enabled()) {
        this.map.scrollWheelZoom.enable();
        this.showTemporaryMessage('Zoom con rueda activado. Haz clic para desactivar.');
      } else {
        this.map.scrollWheelZoom.disable();
      }
    });
    
    // Mejorar la experiencia táctil en dispositivos móviles
    if (L.Browser.mobile) {
      this.map.on('load', () => {
        this.showTemporaryMessage('Usa dos dedos para desplazar el mapa');
      });
    }
  }
  
  /**
   * Muestra un mensaje temporal sobre el mapa
   */
  showTemporaryMessage(message, duration = 3000) {
    const msgElement = document.createElement('div');
    msgElement.className = 'map-message';
    msgElement.textContent = message;
    msgElement.style.cssText = `
      position: absolute;
      bottom: 10px;
      left: 10px;
      background-color: rgba(255, 255, 255, 0.8);
      padding: 5px 10px;
      border-radius: 4px;
      font-size: 14px;
      z-index: 1000;
      pointer-events: none;
    `;
    
    this.mapElement.appendChild(msgElement);
    
    setTimeout(() => {
      msgElement.style.opacity = '0';
      msgElement.style.transition = 'opacity 0.5s ease';
      
      setTimeout(() => {
        msgElement.remove();
      }, 500);
    }, duration);
  }
  
  /**
   * Maneja errores durante la inicialización del mapa
   */
  handleMapError(error) {
    console.error('Error al inicializar mapa:', error);
    
    // Mostrar mensaje de error en lugar del mapa
    const errorMsg = document.createElement('div');
    errorMsg.className = 'map-error-message';
    errorMsg.innerHTML = `
      <div class="alert alert-warning">
        <i class="bi bi-exclamation-triangle-fill me-2"></i>
        <strong>No se pudo cargar el mapa</strong>
        <p class="mb-0 mt-2">Intenta refrescar la página o verifica tu conexión a internet.</p>
      </div>
    `;
    
    // Reemplazar el indicador de carga con el mensaje de error
    if (this.loadingIndicator) {
      this.loadingIndicator.remove();
    }
    
    this.mapElement.appendChild(errorMsg);
  }
}

// Función de inicialización global para la página de detalle de propiedad
function initPropertyMap() {
  return new MapManager('map', {
    latElement: 'lat',
    lonElement: 'lon'
  });
}

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', () => {
  // Comprobar si estamos en la página de detalle de propiedad
  if (document.getElementById('map')) {
    initPropertyMap();
  }
});

// Exportar para uso global
window.MapManager = MapManager;