document.addEventListener('DOMContentLoaded', () => {
  const container = document.getElementById('propiedadesContainer');
  const paginacion = document.getElementById('paginacion');
  const btnFiltrar = document.getElementById('btnFiltrar');

  let pagina = 0, tamaño = 10;

  function cargaDatos() {
    const busq = document.getElementById('busqueda').value;
    const activo = document.getElementById('filtroActivo').value;
    fetch(`/propietario/api/propiedades?busqueda=${encodeURIComponent(busq)}&activo=${activo}&page=${pagina}&size=${tamaño}`)
      .then(res => res.json())
      .then(data => {
        container.innerHTML = '';
        data.content.forEach(p => {
          const card = document.createElement('div');
          card.className = 'col';
          card.innerHTML = `
            <div class="card h-100 shadow-sm">
              <img src="${p.fotoPrincipal}" class="card-img-top" alt="${p.titulo}">
              <div class="card-body">
                <h5 class="card-title">${p.titulo}</h5>
                <p class="card-text">${p.ciudad}, ${p.pais}</p>
                <a href="/propietario/propiedades/${p.id}/editar" class="btn btn-sm btn-outline-primary">Editar</a>
                <button data-id="${p.id}" class="btn btn-sm btn-danger btn-eliminar">Eliminar</button>
              </div>
            </div>`;
          container.append(card);
        });
        // paginación
        let htmlPages = '';
        for (let i = 0; i < data.totalPages; i++) {
          htmlPages += `<li class="page-item ${i===pagina?'active':''}">
                          <a class="page-link" href="#" data-page="${i}">${i+1}</a>
                        </li>`;
        }
        paginacion.innerHTML = `<ul class="pagination justify-content-center">${htmlPages}</ul>`;
      });
  }

  // eventos
  btnFiltrar.addEventListener('click', e => {
    e.preventDefault();
    pagina = 0;
    cargaDatos();
  });

  paginacion.addEventListener('click', e => {
    if (e.target.matches('.page-link')) {
      pagina = +e.target.dataset.page;
      cargaDatos();
    }
  });

  container.addEventListener('click', e => {
    if (e.target.classList.contains('btn-eliminar')) {
      const id = e.target.dataset.id;
      if (confirm('¿Seguro que quieres eliminar esta propiedad?')) {
        fetch(`/propietario/api/propiedades/${id}`, { method: 'DELETE' })
          .then(_ => cargaDatos());
      }
    }
  });

  // primera carga
  cargaDatos();
});
