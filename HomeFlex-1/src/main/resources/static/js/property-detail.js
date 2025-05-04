/**
 * JS para la página de detalle de propiedad
 * Funcionalidades:
 * 1. Inicialización de galería de imágenes con LightGallery
 * 2. Carga diferida (lazy loading) del mapa para mejor rendimiento
 * 3. Ajuste automático de la visualización de la galería según cantidad de imágenes
 */

document.addEventListener('DOMContentLoaded', () => {
  initGallery();
  
  setupMapObserver();
  
  adjustGalleryLayout();
  
  setupDirectGoogleMapsLink();
});

/**
 * Inicializa la galería de imágenes con LightGallery
 */
function initGallery() {
  const galleryElement = document.getElementById('lightgallery');
  
  if (!galleryElement) return;
  
  // Opciones para LightGallery
  const options = {
    speed: 500,
    download: false,
    counter: true,
    selector: '.gallery-item',
    thumbnail: true,
    animateThumb: true,
    showThumbByDefault: false
  };
  
  // Inicializar LightGallery si jQuery y lightGallery están disponibles
  if (typeof $ !== 'undefined' && typeof lightGallery !== 'undefined') {
    try {
      lightGallery(galleryElement, options);
      console.log('Galería inicializada correctamente');
    } catch (error) {
      console.error('Error al inicializar la galería:', error);
    }
  } else {
    console.warn('LightGallery o jQuery no están disponibles');
  }
}

/**
 * Configura un Intersection Observer para cargar el mapa solo cuando sea visible
 * Esto mejora el rendimiento de la página
 */
function setupMapObserver() {
  const mapElement = document.getElementById('map');
  
  if (!mapElement) return;
  
  const mapObserver = new IntersectionObserver((entries) => {
    // Si el mapa es visible en viewport, inicializarlo
    if (entries[0].isIntersecting) {
      initMap();
      mapObserver.unobserve(mapElement);
    }
  }, {
    rootMargin: '200px' // Cargar el mapa cuando esté a 200px de ser visible
  });
  
  // Comenzar a observar el elemento del mapa
  mapObserver.observe(mapElement);
}

/**
 * Inicializa el mapa con Leaflet
 */
function initMap() {
  const mapElement = document.getElementById('map');
  const latElement = document.getElementById('lat');
  const lonElement = document.getElementById('lon');
  
  if (!mapElement || !latElement || !lonElement) {
    console.warn('Elementos necesarios para el mapa no encontrados');
    return;
  }
  
  // Obtener coordenadas
  let lat = parseFloat(latElement.value);
  let lon = parseFloat(lonElement.value);
  
  // Validar que las coordenadas sean números válidos
  if (isNaN(lat) || isNaN(lon)) {
    console.warn('Coordenadas inválidas:', lat, lon);
    
    // Usar coordenadas predeterminadas (Madrid) si faltan o son inválidas
    lat = 40.416775;
    lon = -3.703790;
  }
  
  try {
    // Crear el mapa
    const map = L.map('map', {
      center: [lat, lon],
      zoom: 15,
      scrollWheelZoom: false, 
      updateWhenIdle: true,
      preferCanvas: true,
      renderer: L.canvas()
    });
    
    // Añadir tiles (capa base)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 18,
      minZoom: 3,
      tileSize: 256
    }).addTo(map);
    
    // Crear URL para Google Maps
    const googleMapsUrl = `https://www.google.com/maps/search/?api=1&query=${lat},${lon}`;
    
    // Añadir marcador con popup
    const marker = L.marker([lat, lon]).addTo(map);
    
    // Configurar popup con contenido HTML
    const popupContent = `
      <div class="map-popup">
        <strong>Ver ubicación exacta</strong>
        <p>Abre esta ubicación en Google Maps</p>
        <a href="${googleMapsUrl}" target="_blank" class="btn btn-sm btn-primary">
          <i class="bi bi-google"></i> Abrir Google Maps
        </a>
      </div>
    `;
    
    marker.bindPopup(popupContent);
    
    // Forzar apertura del popup al hacer clic en el marcador
    marker.on('click', function() {
      this.openPopup();
    });
    
    // Añadir listener para habilitar scroll wheel zoom cuando el usuario haga clic en el mapa
    map.on('click', function() {
      if (!map.scrollWheelZoom.enabled()) {
        map.scrollWheelZoom.enable();
        // Mostrar mensaje de ayuda
        const helpMsg = document.createElement('div');
        helpMsg.className = 'map-control-message';
        helpMsg.textContent = 'Zoom activado. Haz clic para desactivar.';
        helpMsg.style.position = 'absolute';
        helpMsg.style.bottom = '10px';
        helpMsg.style.left = '10px';
        helpMsg.style.backgroundColor = 'rgba(255,255,255,0.8)';
        helpMsg.style.padding = '5px 10px';
        helpMsg.style.borderRadius = '4px';
        helpMsg.style.zIndex = 1000;
        mapElement.appendChild(helpMsg);
        
        setTimeout(() => {
          helpMsg.remove();
        }, 3000);
      } else {
        map.scrollWheelZoom.disable();
      }
    });
    
    console.log('Mapa inicializado correctamente');
  } catch (error) {
    console.error('Error al inicializar el mapa:', error);
  }
}

/**
 * Ajusta la visualización de la galería según la cantidad de imágenes
 */
function adjustGalleryLayout() {
  const gallery = document.querySelector('.property-gallery');
  
  if (!gallery) return;
  
  const galleryItems = gallery.querySelectorAll('.gallery-item');
  const itemCount = galleryItems.length;
  
  // Si hay solo una imagen, mostrar en formato grande único
  if (itemCount === 1) {
    gallery.classList.add('single-image');
  }
  // Si hay menos de 5 imágenes, ajustar la cuadrícula
  else if (itemCount < 5) {
    gallery.classList.add('few-images');
  }
  
  // Añadir botón "Ver más fotos" si hay más de 5 imágenes
  if (itemCount > 5) {
    const viewMoreBtn = document.createElement('div');
    viewMoreBtn.className = 'view-more-photos';
    viewMoreBtn.innerHTML = `<span>+${itemCount - 5} fotos más</span>`;
    viewMoreBtn.style.position = 'absolute';
    viewMoreBtn.style.bottom = '10px';
    viewMoreBtn.style.right = '10px';
    viewMoreBtn.style.backgroundColor = 'rgba(0,0,0,0.7)';
    viewMoreBtn.style.color = 'white';
    viewMoreBtn.style.padding = '5px 10px';
    viewMoreBtn.style.borderRadius = '4px';
    viewMoreBtn.style.cursor = 'pointer';
    
    if (galleryItems[5]) {
      galleryItems[5].appendChild(viewMoreBtn);
    }
    
    // Añadir evento para abrir la galería completa
    viewMoreBtn.addEventListener('click', (e) => {
      e.stopPropagation(); // Evitar que se abra la imagen específica
      
      // Simular clic en la primera imagen para abrir la galería
      if (galleryItems[0]) {
        galleryItems[0].click();
      }
    });
  }
}

/**
 * Configura un enlace directo a Google Maps como alternativa al popup
 */
function setupDirectGoogleMapsLink() {
  const mapElement = document.getElementById('map');
  const latElement = document.getElementById('lat');
  const lonElement = document.getElementById('lon');
  const mapInfoSection = document.querySelector('.property-map .mt-2');
  
  if (!mapElement || !latElement || !lonElement || !mapInfoSection) return;
  
  // Obtener coordenadas
  const lat = parseFloat(latElement.value);
  const lon = parseFloat(lonElement.value);
  
  // Validar coordenadas
  if (isNaN(lat) || isNaN(lon)) return;
  
  // Crear URL para Google Maps
  const googleMapsUrl = `https://www.google.com/maps/search/?api=1&query=${lat},${lon}`;
  
  // Crear enlace alternativo
  const directLink = document.createElement('p');
  directLink.className = 'small text-primary ms-auto';
  directLink.innerHTML = `
    <a href="${googleMapsUrl}" target="_blank" class="text-decoration-none">
      <i class="bi bi-box-arrow-up-right"></i> Abrir directamente en Google Maps
    </a>
  `;
  
  // calendario
  document.addEventListener('DOMContentLoaded', () => {
    // Al abrir el modal, inicializamos el calendario
    const modalEl = document.getElementById('calendarioModal');
    modalEl.addEventListener('shown.bs.modal', async () => {
      // Sólo crear una vez
      if (modalEl.dataset.inited) return;
      modalEl.dataset.inited = 'true';

      const calendarEl = document.getElementById('calendar');

      // Obtén el ID de la propiedad (por ejemplo, desde un data-attribute)
      const propiedadId = calendarEl.closest('[data-propiedad-id]').dataset.propiedadId;

      // Crea el calendario
      const calendar = new FullCalendar.Calendar(calendarEl, {
        initialView: 'dayGridMonth',
        locale: 'es',
        headerToolbar: {
          left: 'prev,next today',
          center: 'title',
          right: ''
        },
        events: async (fetchInfo, successCallback, failureCallback) => {
          try {
            // Llamada al endpoint REST que devuelve fechas ocupadas en JSON
            const from = fetchInfo.startStr;
            const to   = fetchInfo.endStr;
            const res  = await fetch(
              `/api/reservas/fechasOcupadas?propiedadId=${propiedadId}&desde=${from}&hasta=${to}`
            );
            const data = await res.json();
            // data = [{ fecha: "2025-06-01", estado: "OCUPADA" }, …]
            
            // Mapear a formato FullCalendar
            const events = data.map(r => ({
              title: r.estado === 'OCUPADA' ? 'Ocupado' : 'Disponible',
              start: r.fecha,
              allDay: true,
              backgroundColor: r.estado === 'OCUPADA' ? 'red' : 'green',
              borderColor:     r.estado === 'OCUPADA' ? 'red' : 'green'
            }));
            successCallback(events);
          } catch (err) {
            console.error(err);
            failureCallback(err);
          }
        }
      });

      calendar.render();
    });
  });
  
  // Añadir enlace a la sección de información del mapa
  mapInfoSection.appendChild(directLink);
}