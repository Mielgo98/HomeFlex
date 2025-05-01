// propietario-propiedades.js

// --- Helpers / configuración ---

// Obtiene el valor de una cookie
console.log("RULANDO")
function getCookie(name) {
  const matches = document.cookie.match(new RegExp(
    "(?:^|; )" +
      name.replace(/([.$?*|{}()\[\]\\\/+^])/g, '\\$1') +
      "=([^;]*)"
  ));
  return matches ? decodeURIComponent(matches[1]) : null;
}

// Llamada genérica a Nominatim para geocodificar una dirección
async function geocode(address, city) {
	console.log("llamando")
  const q = encodeURIComponent(`${address}, ${city}`);
  const url = `https://nominatim.openstreetmap.org/search?format=json&limit=1&q=${q}`;
  const res = await fetch(url, { headers: { 'Accept': 'application/json' } });
  if (!res.ok) throw new Error('Error al geocodificar');
  const data = await res.json();
  if (!data.length) return null;
  return {
    lat: data[0].lat,
    lon: data[0].lon
  };
}

// --- Listado, editar y eliminar propiedades ---

function cargarMisPropiedades(page = 0, size = 20) {
  const jwt = getCookie('jwt_token');
  fetch(`/api/propietario/propiedades?page=${page}&size=${size}`, {
    headers: {
      'Authorization': 'Bearer ' + jwt,
      'Accept': 'application/json'
    }
  })
    .then(res => {
      if (res.status === 401) {
        window.location = '/login';
        throw new Error('No autorizado');
      }
      return res.json();
    })
    .then(data => renderPropiedades(data))
    .catch(err => console.error(err));
}

function renderPropiedades(page) {
  const container = document.getElementById('propiedades-container');
  if (!container) return console.error('No existe #propiedades-container');
  container.innerHTML = '';
  page.content.forEach(prop => {
    const card = document.createElement('div');
    card.className = 'card mb-3';
    card.innerHTML = `
      <div class="row g-0">
        <div class="col-md-4">
          <img src="${prop.fotoPrincipal}" class="img-fluid rounded-start" alt="${prop.titulo}">
        </div>
        <div class="col-md-8">
          <div class="card-body">
            <h5 class="card-title">${prop.titulo}</h5>
            <p class="card-text">${prop.descripcion}</p>
            <p class="card-text"><small class="text-muted">${prop.ciudad}, ${prop.pais}</small></p>
            <button class="btn btn-sm btn-primary btn-editar" data-id="${prop.id}">Editar</button>
            <button class="btn btn-sm btn-danger btn-eliminar" data-id="${prop.id}">Eliminar</button>
          </div>
        </div>
      </div>
    `;
    container.appendChild(card);
  });

  document.querySelectorAll('.btn-editar').forEach(btn =>
    btn.addEventListener('click', () => {
      window.location = `/propietario/propiedades/editar/${btn.dataset.id}`;
    })
  );

  document.querySelectorAll('.btn-eliminar').forEach(btn =>
    btn.addEventListener('click', () => {
      Swal.fire({
        title: '¿Eliminar propiedad?',
        text: "¡Esta acción no se puede deshacer!",
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: 'Sí, eliminar',
        cancelButtonText: 'Cancelar'
      }).then(({ isConfirmed }) => {
        if (isConfirmed) eliminarPropiedad(btn.dataset.id);
      });
    })
  );
}

function eliminarPropiedad(id) {
  const jwt = getCookie('jwt_token');
  fetch(`/api/propietario/propiedades/${id}`, {
    method: 'DELETE',
    headers: {
      'Authorization': 'Bearer ' + jwt
    }
  })
    .then(res => {
      if (res.status === 401) {
        window.location = '/login';
        throw new Error('No autorizado');
      }
      if (!res.ok) throw new Error('Error al eliminar');
    })
    .then(() => {
      Swal.fire({
        title: '¡Eliminada!',
        text: 'Tu propiedad ha sido eliminada.',
        icon: 'success',
        timer: 2000,
        showConfirmButton: false
      });
      cargarMisPropiedades();
    })
    .catch(err => {
      console.error(err);
      Swal.fire({
        title: 'Error',
        text: 'No se pudo eliminar la propiedad.',
        icon: 'error'
      });
    });
}

// --- Geocodificación y envío de formulario de creación/edición ---

async function geocodeAndSubmit(event) {
  event.preventDefault();
  const form = event.target;
  const address = form.querySelector('input[name="direccion"]').value.trim();
  const city    = form.querySelector('input[name="ciudad"]').value.trim();
  if (!address || !city) {
    return Swal.fire('Faltan datos', 'Debes indicar dirección y ciudad.', 'warning');
  }

  Swal.fire({
    title: 'Obteniendo coordenadas…',
    allowOutsideClick: false,
    didOpen: () => Swal.showLoading()
  });

  try {
    const pos = await geocode(address, city);
    if (!pos) {
      Swal.close();
      return Swal.fire('No encontrado', 'No hemos podido geocodificar esa dirección.', 'error');
    }
    // Rellenamos campos ocultos
    form.querySelector('input[name="latitud"]').value  = pos.lat;
    form.querySelector('input[name="longitud"]').value = pos.lon;

    // Ahora enviamos el formulario (por defecto POST/PUT)
    form.submit();

  } catch (err) {
    console.error(err);
    Swal.fire('Error', 'Hubo un problema al consultar la API de geocodificación.', 'error');
  }
}

// --- Inicialización al cargar la página ---

document.addEventListener('DOMContentLoaded', () => {
  cargarMisPropiedades();

  // Si existe el formulario de propiedad, le engancha el submit
  const propForm = document.getElementById('propiedad-form');
  if (propForm) {
    propForm.addEventListener('submit', geocodeAndSubmit);
  }
});
