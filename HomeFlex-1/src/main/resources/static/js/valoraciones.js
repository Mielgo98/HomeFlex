// valoraciones.js

// ── 1. Leer ID de usuario actual ─────────────────────────────
let currentUserId = null;
const userElem = document.getElementById('userData');
if (userElem) currentUserId = parseInt(userElem.dataset.currentUserId, 10);
console.log('💡 currentUserId leido del DOM:', currentUserId);

// ── 2. Referencias a elementos ────────────────────────────────
const valoracionesContainer = document.getElementById('valoraciones-list');
const btnOpenReviewModal    = document.getElementById('btnOpenReviewModal');
const reviewModalEl         = document.getElementById('reviewModal');
const reviewModal           = new bootstrap.Modal(reviewModalEl);
const reviewCommentEl       = document.getElementById('reviewComment');
const submitBtn             = document.getElementById('btnSubmitReview');

// ── 3. Configurar grupos de estrellas ─────────────────────────
const groups = {
  general:      { el: document.getElementById('ratingGeneral'),      value: 0 },
  limpieza:     { el: document.getElementById('ratingLimpieza'),     value: 0 },
  ubicacion:    { el: document.getElementById('ratingUbicacion'),    value: 0 },
  comunicacion: { el: document.getElementById('ratingComunicacion'), value: 0 },
  calidad:      { el: document.getElementById('ratingCalidad'),      value: 0 },
};
for (const [key, grp] of Object.entries(groups)) {
  const stars = Array.from(grp.el.querySelectorAll('span'));
  stars.forEach(star => {
    star.addEventListener('click', () => {
      grp.value = Number(star.dataset.value);
      stars.forEach(s =>
        s.classList.toggle('selected', Number(s.dataset.value) <= grp.value)
      );
      console.log(`💡 ${key} =`, grp.value);
    });
  });
}

// ── 4. Obtener y renderizar valoraciones ───────────────────────

// Función para parsear distintos formatos de fecha
function parseDate(raw) {
  if (!raw) return '';
  let dateObj;
  if (typeof raw === 'string') {
    // reemplazamos espacio por 'T' en caso de llegar "2025-05-03 14:23:44"
    dateObj = new Date(raw.replace(' ', 'T'));
  } else if (typeof raw === 'number') {
    // timestamp en milisegundos o segundos
    dateObj = new Date(raw);
  } else {
    // podría ser Date serializado, intentamos construír de todos modos
    dateObj = new Date(raw);
  }
  return isNaN(dateObj.getTime())
    ? ''
    : dateObj.toLocaleDateString();
}

function fetchValoraciones() {
  console.log('💡 Llamando a fetchValoraciones para propiedad', propiedadId);
  valoracionesContainer.innerHTML = `
    <div class="spinner-border" role="status">
      <span class="visually-hidden">Cargando…</span>
    </div>
  `;

  fetch(`/api/valoraciones/propiedad/${propiedadId}`)
    .then(res => res.ok ? res.json() : Promise.reject(res.status))
    .then(data => {
      const list = Array.isArray(data) ? data : (data.content || []);
      if (!list.length) {
        valoracionesContainer.innerHTML = `
          <p class="alert alert-info">Sé el primero en valorar esta propiedad.</p>
        `;
        return;
      }

      valoracionesContainer.innerHTML = list.map(v => {
        const fechaCreacion = parseDate(v.fechaCreacion);
        const fechaRespuesta = parseDate(v.fechaRespuesta);
        const respuestaHTML = v.respuestaPropietario
          ? `<div class="mt-3 p-3 respuesta-propietario bg-light rounded">
               <strong>Respuesta del anfitrión</strong>
               <small class="text-muted d-block mb-1">${fechaRespuesta}</small>
               <p class="mb-0">${v.respuestaPropietario}</p>
             </div>`
          : '';

        const isMine = currentUserId && v.usuarioId === currentUserId;
        return `
          <div class="card mb-3" data-review-id="${v.id}">
            <div class="card-body">
              <h5>${v.usuarioNombre}</h5>
              <div class="star-rating mb-2">
                ${'★'.repeat(v.puntuacion)}${'☆'.repeat(5 - v.puntuacion)}
              </div>
              <p>${v.comentario}</p>
              <small class="text-muted">${fechaCreacion}</small>
              ${respuestaHTML}
              ${isMine ? `
                <div class="review-actions mt-2">
                  <button class="btn btn-sm btn-outline-primary btn-edit-review" data-id="${v.id}">
                    Editar
                  </button>
                  <button class="btn btn-sm btn-outline-danger btn-delete-review" data-id="${v.id}">
                    Eliminar
                  </button>
                </div>` 
              : ''}
            </div>
          </div>
        `;
      }).join('');
    })
    .catch(err => {
      console.error('❌ Error en fetchValoraciones:', err);
      valoracionesContainer.innerHTML = `
        <p class="alert alert-danger">
          No se pudieron cargar las valoraciones.
        </p>
      `;
    });
}

// ── 5. Abrir modal de nueva/editar ─────────────────────────────
btnOpenReviewModal.addEventListener('click', () => {
  reviewModalEl.querySelector('.modal-title').textContent = 'Nueva valoración';
  reviewCommentEl.value = '';
  delete submitBtn.dataset.editId;
  for (const grp of Object.values(groups)) {
    grp.value = 0;
    grp.el.querySelectorAll('span').forEach(s => s.classList.remove('selected'));
  }
  reviewModal.show();
});

// ── 6. Editar / Eliminar ───────────────────────────────────────
valoracionesContainer.addEventListener('click', e => {
  // Editar
  if (e.target.matches('.btn-edit-review')) {
    const id = e.target.dataset.id;
    fetch(`/api/valoraciones/${id}`)
      .then(r => r.json())
      .then(v => {
        reviewModalEl.querySelector('.modal-title').textContent = 'Editar valoración';
        reviewCommentEl.value = v.comentario;
        submitBtn.dataset.editId = id;
        for (const [key, grp] of Object.entries(groups)) {
          grp.value = v[key] ?? (key === 'general' ? v.puntuacion : 0);
          grp.el.querySelectorAll('span').forEach(s =>
            s.classList.toggle('selected', Number(s.dataset.value) <= grp.value)
          );
        }
        reviewModal.show();
      });
    return;
  }

  // Eliminar
  if (e.target.matches('.btn-delete-review')) {
    const id = e.target.dataset.id;
    Swal.fire({
      title: '¿Eliminar tu valoración?',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Sí, eliminar',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed) {
        fetch(`/api/valoraciones/${id}`, { method: 'DELETE' })
          .then(r => {
            if (!r.ok) throw new Error(r.status);
            fetchValoraciones();
            Swal.fire('Eliminada', 'Tu valoración ha sido eliminada.', 'success');
          })
          .catch(err => {
            console.error('❌ Error al eliminar:', err);
            Swal.fire('Error', 'No se pudo eliminar la valoración.', 'error');
          });
      }
    });
  }
});

// ── 7. Enviar (crear o actualizar) ────────────────────────────
submitBtn.addEventListener('click', () => {
  const comentario = reviewCommentEl.value.trim();
  if (!comentario || groups.general.value === 0) {
    return Swal.fire('Atención', 'Debes escribir un comentario y elegir puntuación global.', 'warning');
  }

  const editId = submitBtn.dataset.editId;
  const method = editId ? 'PUT' : 'POST';
  const url    = editId
    ? `/api/valoraciones/${editId}`
    : `/api/valoraciones/propiedad/${propiedadId}`;

  const body = {
    comentario,
    puntuacion:   groups.general.value,
    limpieza:     groups.limpieza.value || null,
    ubicacion:    groups.ubicacion.value || null,
    comunicacion: groups.comunicacion.value || null,
    calidad:      groups.calidad.value || null
  };

  console.log(`💡 Enviando ${method} a ${url}`, body);
  fetch(url, {
    method,
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body)
  })
  .then(r => {
    if (!r.ok) throw new Error(r.status);
    reviewModal.hide();
    fetchValoraciones();
    Swal.fire('Guardado', 'Tu valoración ha sido guardada.', 'success');
  })
  .catch(err => {
    console.error('❌ Error al guardar valoración:', err);
    Swal.fire('Error', 'No se pudo guardar la valoración.', 'error');
  });
});

// ── 8. Inicializar ────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', fetchValoraciones);
