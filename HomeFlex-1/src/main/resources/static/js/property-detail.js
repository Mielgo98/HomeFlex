// src/main/resources/static/js/property-detail.js

// 1) Extrae toda la lógica de Leaflet a una función initMap
function initMap() {
  const latEl = document.getElementById('lat');
  const lonEl = document.getElementById('lon');
  if (!latEl || !lonEl) return;

  const lat = parseFloat(latEl.value);
  const lon = parseFloat(lonEl.value);
  if (isNaN(lat) || isNaN(lon)) return;

  // Inicializa el mapa
  const map = L.map('map', {
    center: [lat, lon],
    zoom: 13,
    // Opciones para reducir peticiones extra:
    updateWhenIdle: true,
    updateWhenZooming: false,
    reuseTiles: true,
    maxZoom: 16,
    detectRetina: false
  });

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap contributors',
    // igual aquí puedes limitar a z entre 0 y 16
    maxZoom: 16,
    detectRetina: false
  }).addTo(map);

  L.marker([lat, lon]).addTo(map);

  // fuerza re-cálculo al cargar totalmente
  window.addEventListener('load', () => map.invalidateSize());
}

// 2) Espera a que el DOM esté listo para montar el observer
document.addEventListener('DOMContentLoaded', () => {
  const mapEl = document.getElementById('map');
  if (!mapEl) return;

  // 3) Crea el observer que inicializará el mapa sólo cuando sea visible
  const observer = new IntersectionObserver((entries, obs) => {
    if (entries[0].isIntersecting) {
      initMap();
      obs.unobserve(mapEl);
    }
  }, {
    rootMargin: '200px'  // adelanta un poco la carga
  });

  observer.observe(mapEl);
});
