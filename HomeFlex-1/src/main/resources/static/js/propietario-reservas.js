document.addEventListener('DOMContentLoaded', () => {
  const cont = document.getElementById('reservasContainer');
  const pag = document.getElementById('paginacionRes');
  const btnFil = document.getElementById('btnFiltrarRes');

  let pagina = 0, tamaño = 10;

  function cargar() {
    const estado = document.getElementById('filtroEstado').value;
    const busq   = document.getElementById('busquedaRes').value;
    fetch(`/propietario/api/reservas?estado=${estado}&busqueda=${encodeURIComponent(busq)}&page=${pagina}&size=${tamaño}`)
      .then(r => r.json())
      .then(data => {
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
              <button data-id="${r.id}" data-action="aprobar" class="btn btn-success">Aprobar</button>
              <button data-id="${r.id}" data-action="rechazar" class="btn btn-danger">Rechazar</button>
              <button data-id="${r.id}" data-action="confirmar" class="btn btn-primary">Confirmar</button>
            </div>`;
          cont.append(div);
        });
        // paginación igual que antes…
        let html = '';
        for (let i=0; i<data.totalPages; i++) {
          html += `<li class="page-item ${i===pagina?'active':''}">
                     <a href="#" class="page-link" data-page="${i}">${i+1}</a>
                   </li>`;
        }
        pag.innerHTML = `<ul class="pagination justify-content-center">${html}</ul>`;
      });
  }

  btnFil.addEventListener('click', e => { e.preventDefault(); pagina=0; cargar(); });
  pag.addEventListener('click', e => {
    if (e.target.matches('.page-link')) {
      pagina = +e.target.dataset.page;
      cargar();
    }
  });

  cont.addEventListener('click', e => {
    if (e.target.matches('button[data-action]')) {
      const id = e.target.dataset.id;
      const act= e.target.dataset.action;
      fetch(`/propietario/api/reservas/${id}/${act}`, { method: 'POST' })
        .then(_ => cargar());
    }
  });

  cargar();
});
