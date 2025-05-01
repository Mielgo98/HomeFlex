// propietario-form.js

// Asegúrate de incluir este script con defer o al final del body:
// <script src="/js/propietario-form.js" defer></script>

;(function() {
  // Helper: muestra SweetAlert de carga
  function showLoading(title = 'Por favor espera…') {
    Swal.fire({
      title,
      allowOutsideClick: false,
      didOpen: () => Swal.showLoading()
    });
  }

  // Helper: geocodifica con Nominatim
  async function geocode(address, city) {
    console.log('[propietario-form] geocode →', address, city);
    const q   = encodeURIComponent(`${address}, ${city}`);
    const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${q}`;
    const res = await fetch(url, { headers: { 'Accept': 'application/json' } });
    if (!res.ok) throw new Error('Error al geocodificar');
    const data = await res.json();
    if (!data.length) return null;
    return { lat: data[0].lat, lon: data[0].lon };
  }

  // Manejador del submit del formulario de propiedad
  async function onFormSubmit(evt) {
    evt.preventDefault();
    const form    = evt.target;
    const address = form.querySelector('input[name="direccion"]').value.trim();
    const city    = form.querySelector('input[name="ciudad"]').value.trim();

    if (!address || !city) {
      return Swal.fire('Faltan datos', 'Debe indicar dirección y ciudad.', 'warning');
    }

    showLoading('Obteniendo coordenadas…');

    try {
      const pos = await geocode(address, city);
      if (!pos) {
        Swal.close();
        return Swal.fire('No encontrado',
                         'No hemos podido geocodificar esa dirección.',
                         'error');
      }

      // Rellenamos los campos ocultos
      form.querySelector('input[name="latitud"]').value  = pos.lat;
      form.querySelector('input[name="longitud"]').value = pos.lon;

      // Una vez tenemos coords, enviamos
      form.submit();

    } catch (e) {
      console.error('[propietario-form] geocode error', e);
      Swal.close();
      Swal.fire('Error', 'Hubo un problema con la geocodificación.', 'error');
    }
  }

  // Al cargar el DOM, enganchamos listener
  document.addEventListener('DOMContentLoaded', () => {
    console.log('[propietario-form] DOM listo');
    const form = document.getElementById('propiedad-form');
    if (!form) {
      console.warn('[propietario-form] No existe <form id="propiedad-form">');
      return;
    }
    form.addEventListener('submit', onFormSubmit);
    console.log('[propietario-form] Listener de submit enganchado');
  });
})();
