// Obtiene el valor de una cookie
function getCookie(name) {
  const matches = document.cookie.match(new RegExp(
    "(?:^|; )" +
      name.replace(/([.$?*|{}()\[\]\\\/+^])/g, '\\$1') +
      "=([^;]*)"
  ));
  return matches ? decodeURIComponent(matches[1]) : null;
}

// --- Funciones principales ---

// Carga la lista de propiedades del propietario
async function cargarMisPropiedades(page = 0, size = 20) {
  const jwt = getCookie('jwt_token');
  try {
    const res = await fetch(`/api/propietario/propiedades?page=${page}&size=${size}`, {
      headers: {
        'Authorization': 'Bearer ' + jwt,
        'Accept': 'application/json'
      }
    });
    if (res.status === 401) {
      window.location = '/login';
      return;
    }
    const data = await res.json();
    renderPropiedades(data);
  } catch (err) {
    console.error(err);
    Swal.fire('Error', 'No se pudieron cargar tus propiedades.', 'error');
  }
}

// Renderiza las tarjetas de propiedades
function renderPropiedades(page) {
  const container = document.getElementById('propiedades-container');
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
            <button class="btn btn-sm btn-info btn-view-reviews" data-id="${prop.id}">Ver valoraciones</button>
          </div>
        </div>
      </div>
    `;
    container.appendChild(card);
  });

  // Delegar eventos de editar/eliminar/ver valoraciones
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
  document.querySelectorAll('.btn-view-reviews').forEach(btn =>
    btn.addEventListener('click', () => abrirModalValoraciones(btn.dataset.id))
  );
}

// Elimina una propiedad
function eliminarPropiedad(id) {
  const jwt = getCookie('jwt_token');
  fetch(`/api/propietario/propiedades/${id}`, {
    method: 'DELETE',
    headers: { 'Authorization': 'Bearer ' + jwt }
  })
    .then(res => {
      if (res.status === 401) window.location = '/login';
      if (!res.ok) throw new Error('Error al eliminar');
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
      Swal.fire('Error', 'No se pudo eliminar la propiedad.', 'error');
    });
}

// --- Modal de valoraciones y respuestas ---

const reviewsModalEl = document.getElementById('reviewsModal');
const reviewsModal   = new bootstrap.Modal(reviewsModalEl);
const reviewsList    = document.getElementById('reviews-list');

function abrirModalValoraciones(propiedadId) {
  // Ajustar título del modal
  reviewsModalEl.querySelector('.modal-title').textContent =
    `Valoraciones de la propiedad #${propiedadId}`;

  // Mostrar loader
  reviewsList.innerHTML = `
    <div class="text-center my-4">
      <div class="spinner-border" role="status"></div>
    </div>`;

  const jwt = getCookie('jwt_token');
  fetch(`/api/valoraciones/propiedad/${propiedadId}`, {
    headers: { 'Authorization': 'Bearer ' + jwt }
  })
    .then(res => res.ok ? res.json() : Promise.reject(res.status))
    .then(page => {
      const vals = page.content || [];
      if (!vals.length) {
        reviewsList.innerHTML = `<p class="text-center">No hay valoraciones aún.</p>`;
      } else {
        reviewsList.innerHTML = vals.map(v => {
          const fechaVal = new Date(v.fechaCreacion).toLocaleDateString();
          const stars    = '★'.repeat(v.puntuacion) + '☆'.repeat(5 - v.puntuacion);

          if (v.respuestaPropietario) {
            const fechaResp = new Date(v.fechaRespuesta).toLocaleDateString();
            return `
              <div class="mb-4">
                <h6>${v.usuarioNombre} <small class="text-muted">${fechaVal}</small></h6>
                <p>${stars}</p>
                <p>${v.comentario}</p>
                <div class="border rounded p-2 bg-light">
                  <strong>Tu respuesta</strong> <small class="text-muted">${fechaResp}</small>
                  <p>${v.respuestaPropietario}</p>
                </div>
              </div>
            `;
          } else {
            return `
              <div class="mb-4">
                <h6>${v.usuarioNombre} <small class="text-muted">${fechaVal}</small></h6>
                <p>${stars}</p>
                <p>${v.comentario}</p>
                <textarea id="resp-${v.id}"
                          class="form-control mb-2"
                          placeholder="Escribe tu respuesta..."></textarea>
                <button class="btn btn-sm btn-success btn-send-response"
                        data-id="${v.id}"
                        data-prop="${propiedadId}">
                  Enviar respuesta
                </button>
              </div>
            `;
          }
        }).join('');

        // Enlazar envío de respuesta
        reviewsList.querySelectorAll('.btn-send-response').forEach(btn => {
          btn.addEventListener('click', () => {
            const reviewId  = btn.dataset.id;
            const propId    = btn.dataset.prop;
            const respuesta = document.getElementById(`resp-${reviewId}`).value.trim();

            // Validar longitud mínima
            if (!respuesta || respuesta.length < 10) {
              return Swal.fire('Atención', 'La respuesta debe tener al menos 10 caracteres.', 'info');
            }

            // Construir cuerpo con clave valoracionId
            fetch('/api/valoraciones/responder', {
              method: 'POST',
              headers: {
                'Authorization': 'Bearer ' + jwt,
                'Content-Type': 'application/json'
              },
              body: JSON.stringify({
                valoracionId: reviewId,
                respuesta: respuesta
              })
            })
            .then(r => r.ok ? r.json() : Promise.reject(r.status))
            .then(() => {
              Swal.fire('¡Listo!', 'Respuesta enviada correctamente.', 'success');
              abrirModalValoraciones(propId);  // recarga el modal
            })
            .catch(err => {
              console.error(err);
              Swal.fire('Error', 'No se pudo enviar la respuesta.', 'error');
            });
          });
        });
      }
    })
    .catch(() => {
      Swal.fire('Error', 'No se pudieron cargar las valoraciones.', 'error');
      reviewsList.innerHTML = '';
    });

  reviewsModal.show();
}

// --- Inicialización ---

document.addEventListener('DOMContentLoaded', () => {
  cargarMisPropiedades();
});
