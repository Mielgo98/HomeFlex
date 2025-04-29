/**
 * JS para la página de listado de propiedades de HomeFlex
 * - Filtros y animaciones
 * - Paginación totalmente asíncrona (fetch + DOMParser + delegación)
 */

document.addEventListener('DOMContentLoaded', () => {
  initFilters();
  animatePropertyCards();
  initPagination();
});

/**
 * Inicializa el manejo de filtros y su comportamiento
 */
function initFilters() {
  const filtersForm = document.getElementById('filtersForm');
  if (!filtersForm) return;

  // Filtrado automático al cambiar ordenación
  const ordenarSelect = document.getElementById('ordenar');
  if (ordenarSelect) {
    ordenarSelect.addEventListener('change', () => filtersForm.submit());
  }

  // Sugerencias de ciudades populares
  const ciudadInput = document.getElementById('ciudad');
  const popularCities = document.querySelectorAll('.popular-cities a');
  if (ciudadInput && popularCities.length > 0) {
    popularCities.forEach(city => {
      city.addEventListener('click', e => {
        e.preventDefault();
        ciudadInput.value = city.textContent.trim();
        filtersForm.submit();
      });
    });
    ciudadInput.addEventListener('keypress', e => {
      if (e.key === 'Enter') {
        e.preventDefault();
        filtersForm.submit();
      }
    });
  }

  // Botón para limpiar filtros
  const clearButton = document.querySelector('.clear-filters');
  if (clearButton) {
    clearButton.addEventListener('click', () => {
      filtersForm.reset();
      window.location.href = '/propiedades';
    });
  }
}

/**
 * Aplica animaciones de entrada a las tarjetas de propiedades
 */
function animatePropertyCards() {
  // Selecciona las tarjetas dentro del grid
  const cards = document.querySelectorAll('#gridContainer .card');
  if (cards.length === 0) return;

  cards.forEach((card, index) => {
    card.style.opacity = '0';
    card.style.animationDelay = `${index * 0.1}s`;

    const observer = new IntersectionObserver(entries => {
      if (entries[0].isIntersecting) {
        card.style.opacity = '1';
        observer.unobserve(card);
      }
    }, { threshold: 0.1 });

    observer.observe(card);
  });
}

/**
 * Paginación asíncrona usando delegación
 */
function initPagination() {
  const gridContainer       = document.getElementById('gridContainer');
  const paginationContainer = document.getElementById('paginationContainer');

  // Extrae página actual, total y tamaño
  function parsePagination() {
    const [currEl, totalEl] = paginationContainer.querySelectorAll('.page-info strong');
    const currentPage = parseInt(currEl.textContent, 10) - 1;
    const totalPages  = parseInt(totalEl.textContent, 10);
    const size        = window.pageSize || 9;
    return { currentPage, totalPages, size };
  }

  // Carga una página via AJAX y reemplaza solo el grid + paginación
  async function loadPage(pagina, size) {
    const url = new URL(window.location.origin + '/propiedades');
    url.searchParams.set('pagina', pagina);
    url.searchParams.set('size', size);
    // Preservar filtros en la query string
    new URLSearchParams(window.location.search).forEach((v, k) => {
      if (['ciudad','ordenar'].includes(k)) url.searchParams.set(k, v);
    });

    const res = await fetch(url, {
      headers: { 'X-Requested-With': 'XMLHttpRequest' }
    });
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const html = await res.text();
    const doc  = new DOMParser().parseFromString(html, 'text/html');

    // Sustituir contenido
    gridContainer.innerHTML       = doc.getElementById('gridContainer').innerHTML;
    paginationContainer.innerHTML = doc.getElementById('paginationContainer').innerHTML;

    // Actualizar botones habilitados/deshabilitados
    const { currentPage, totalPages } = parsePagination();
    // (La delegación en container seguirá capturando clicks)
    // Reaplicar animaciones a las nuevas tarjetas
    animatePropertyCards();
  }

  // Delegación de click
  paginationContainer.addEventListener('click', e => {
    const target = e.target;
    const { currentPage, totalPages, size } = parsePagination();

    if (target.id === 'prevBtn' && !target.disabled && currentPage > 0) {
      loadPage(currentPage - 1, size);
    }
    if (target.id === 'nextBtn' && !target.disabled && currentPage + 1 < totalPages) {
      loadPage(currentPage + 1, size);
    }
  });
}
