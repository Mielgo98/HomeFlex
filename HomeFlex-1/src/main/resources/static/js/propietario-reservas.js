
function getCookie(name) {
  const matches = document.cookie.match(new RegExp(
    "(?:^|; )" +
    name.replace(/([.$?*|{}()[\]\\\/+^])/g, '\\$1') +
    "=([^;]*)"
  ));
  return matches ? decodeURIComponent(matches[1]) : null;
}

// --- Carga paginada de reservas ---
function cargarMisReservas(page = 0, size = 20) {
  const jwt = getCookie('jwt_token');
  fetch(`/api/propietario/reservas?page=${page}&size=${size}`, {
    headers: { 'Authorization': 'Bearer ' + jwt }
  })
    .then(res => {
      if (res.status === 401) {
        window.location = '/login';
        throw new Error('No autorizado');
      }
      return res.json();
    })
    .then(data => renderReservas(data))
    .catch(err => console.error('Error al cargar reservas:', err));
}

// --- Renderizado dinámico según estado ---
function renderReservas(page) {
  const container = document.getElementById('reservas-container');
  if (!container) return console.error('Falta #reservas-container');
  container.innerHTML = '';

  page.content.forEach(res => {
    const div = document.createElement('div');
    div.className = 'reserva-item mb-3 p-3 border rounded';
    div.innerHTML = `
      <div class="d-flex justify-content-between align-items-center">
        <div>
          <strong>${res.codigo}</strong> — ${res.propiedad.titulo}<br>
          <small>${res.fechaInicio} a ${res.fechaFin}</small>
        </div>
        <span class="badge bg-secondary">${res.estado}</span>
      </div>
      <div class="mt-2 acciones"></div>
    `;

    const acciones = div.querySelector('.acciones');
    switch (res.estado) {
      case 'PENDIENTE_PAGO':
        acciones.innerHTML = `
          <button class="btn btn-success btn-aprobar me-1" data-id="${res.id}">Aprobar</button>
          <button class="btn btn-danger btn-rechazar" data-id="${res.id}">Rechazar</button>
        `;
        break;
      case 'APROBADA':
        acciones.innerHTML = `
          <button class="btn btn-primary btn-confirmar me-1" data-id="${res.id}">Confirmar</button>
          <button class="btn btn-secondary btn-cancelar" data-id="${res.id}">Cancelar</button>
        `;
        break;
      default:
        acciones.innerHTML = `
          <button class="btn btn-info btn-ver" data-id="${res.id}">Ver</button>
        `;
    }

    container.appendChild(div);
  });
}

// --- Delegación de eventos para acciones ---
document.getElementById('reservas-container')
  .addEventListener('click', evt => {
    const btn = evt.target;
    const id  = btn.dataset.id;
    if (!id) return;

    if (btn.matches('.btn-aprobar')) {
      modificarEstado(id, 'aprobar');
    } else if (btn.matches('.btn-rechazar')) {
      modificarEstado(id, 'rechazar');
    } else if (btn.matches('.btn-confirmar')) {
      modificarEstado(id, 'confirmar');
    } else if (btn.matches('.btn-cancelar')) {
      modificarEstado(id, 'cancelar');
    } else if (btn.matches('.btn-ver')) {
      window.location = `/propietario/reservas/${id}`; // o donde muestres el detalle
    }
  });

// --- Función genérica para cambiar estado con SweetAlert ---
function modificarEstado(id, accion) {
  const jwt = getCookie('jwt_token');
  const titles = {
    aprobar:  ['¿Aprobar reserva?', 'Esta acción confirmará el pago.'],
    rechazar: ['¿Rechazar reserva?', 'El huésped será notificado.'],
    confirmar:['¿Confirmar reserva?', 'La estancia quedará definitiva.'],
    cancelar: ['¿Cancelar reserva?', 'La reserva se marcará como cancelada.']
  };

  const [title, text] = titles[accion] || ['¿Continuar?', ''];

  Swal.fire({
    title,
    text,
    icon: 'warning',
    showCancelButton: true,
    confirmButtonText: 'Sí',
    cancelButtonText: 'No'
  }).then(({ isConfirmed }) => {
    if (!isConfirmed) return;

    fetch(`/api/reservas/${id}/${accion}`, {
      method: 'POST',
      headers: {
        'Authorization': 'Bearer ' + jwt,
        'Content-Type': 'application/json'
      }
    })
      .then(res => {
        if (res.status === 401) {
          window.location = '/login';
          throw new Error('No autorizado');
        }
        if (!res.ok) throw new Error('Fallo al ' + accion);
        return res.json();
      })
      .then(() => {
        Swal.fire({
          title: '¡Listo!',
          text: `Reserva ${accion}da con éxito.`,
          icon: 'success',
          timer: 1500,
          showConfirmButton: false
        });
        cargarMisReservas(); // refrescar lista
      })
      .catch(err => {
        console.error(err);
        Swal.fire('Error', err.message, 'error');
      });
  });
}

// --- Inicialización al cargar la página ---
document.addEventListener('DOMContentLoaded', () => {
  if (document.getElementById('reservas-container')) {
    cargarMisReservas();
  }
});
