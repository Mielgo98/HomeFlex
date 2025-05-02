
// Obtiene el valor de una cookie
function getCookie(name) {
  const matches = document.cookie.match(new RegExp(
    "(?:^|; )" +
    name.replace(/([.$?*|{}()[\]\\\/+^])/g, '\\$1') +
    "=([^;]*)"
  ));
  return matches ? decodeURIComponent(matches[1]) : null;
}

document.addEventListener('DOMContentLoaded', () => {
  const token   = getCookie('jwt_token');
  const cont    = document.getElementById('reservasContainer');
  const pag     = document.getElementById('paginacionRes');
  const btnFil  = document.getElementById('btnFiltrarRes');

  let pagina = 0, tamaño = 10;

  // Lanza la petición GET y pinta las reservas
  function cargar() {
    const estado = document.getElementById('filtroEstado').value;
    const busq   = document.getElementById('busquedaRes').value.trim();

    fetch(`/api/reservas/solicitudes?estado=${estado}&busqueda=${encodeURIComponent(busq)}&page=${pagina}&size=${tamaño}`, {
      headers: {
        'Authorization': 'Bearer ' + token,
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
    .then(data => {
      // Renderizado de tarjetas de reserva
      cont.innerHTML = '';
      data.content.forEach(r => {
        const div = document.createElement('div');
        div.className = 'list-group-item';
        div.innerHTML = `
          <div class="d-flex w-100 justify-content-between">
            <h5 class="mb-1">${r.codigoReserva} – ${r.tituloPropiedad}</h5>
            <small>${r.estado}</small>
          </div>
          <p class="mb-1">${r.fechaInicio} a ${r.fechaFin}</p>
          <div class="btn-group btn-group-sm">
            <a href="/propietario/reservas/${r.id}" class="btn btn-outline-info">Ver</a>
            <button data-id="${r.id}" data-action="aprobar"   class="btn btn-success">Aprobar</button>
            <button data-id="${r.id}" data-action="rechazar"   class="btn btn-danger">Rechazar</button>
            <button data-id="${r.id}" data-action="confirmar"  class="btn btn-primary">Confirmar</button>
            <button data-id="${r.id}" data-action="cancelar"   class="btn btn-secondary">Cancelar</button>
          </div>`;
        cont.append(div);
      });

      // Paginación
      let html = '';
      for (let i = 0; i < data.totalPages; i++) {
        html += `<li class="page-item ${i===pagina?'active':''}">
                   <a href="#" class="page-link" data-page="${i}">${i+1}</a>
                 </li>`;
      }
      pag.innerHTML = `<ul class="pagination justify-content-center">${html}</ul>`;
    })
    .catch(err => {
      console.error(err);
      Swal.fire('Error', 'No se han podido cargar las reservas.', 'error');
    });
  }

  // Filtrar
  btnFil.addEventListener('click', e => {
    e.preventDefault();
    pagina = 0;
    cargar();
  });

  // Paginación
  pag.addEventListener('click', e => {
    if (e.target.matches('.page-link')) {
      pagina = +e.target.dataset.page;
      cargar();
    }
  });

  // Acciones en delegado
  cont.addEventListener('click', e => {
    const btn = e.target;
    if (!btn.matches('button[data-action]')) return;

    const id  = btn.dataset.id;
    const act = btn.dataset.action;

    // Configura la confirmación
    const cfg = {
      aprobar:   { title:'¿Aprobar solicitud?',   text:'La reserva pasará a PENDIENTE_PAGO.', icon:'warning' },
      rechazar:  { title:'¿Rechazar solicitud?',  text:'La reserva se cancelará.',        icon:'warning' },
      confirmar: { title:'¿Confirmar reserva?',   text:'La reserva se confirmará.',       icon:'question' },
      cancelar:  { title:'¿Cancelar reserva?',    text:'La reserva se marcará como cancelada.', icon:'warning' }
    }[act];

    Swal.fire({
      title:            cfg.title,
      text:             cfg.text,
      icon:             cfg.icon,
      showCancelButton: true,
      confirmButtonText:'Sí',
      cancelButtonText: 'No'
    }).then(({ isConfirmed }) => {
      if (!isConfirmed) return;

      fetch(`/api/reservas/${id}/${act}`, {
        method: 'POST',
        headers: {
          'Authorization': 'Bearer ' + token,
          'Accept':        'application/json'
        }
      })
      .then(res => {
        if (!res.ok) throw new Error('Error en la operación');
        return res.json();
      })
      .then(() => {
        Swal.fire('¡Listo!', 'Operación realizada con éxito.', 'success');

        // **** Aquí ajustamos los botones según la acción ****
        const btnGroup = btn.closest('.btn-group');
        if (act === 'aprobar') {
          // dejar solo "Rechazar"
          btnGroup.querySelectorAll('button').forEach(b => {
            if (b.dataset.action !== 'rechazar') b.remove();
          });
        } else if (act === 'rechazar' || act === 'cancelar') {
          // quitar todos los botones
          btnGroup.innerHTML = '';
        }
      })
      .catch(err => {
        console.error(err);
        Swal.fire('Error', 'No se ha podido completar la operación.', 'error');
      });
    });
  });

  // Carga inicial
  cargar();
});
