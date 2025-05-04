/**
 * JS para la página de listado de propiedades de HomeFlex
 * - Filtros y animaciones
 * - Paginación totalmente asíncrona (fetch + DOMParser + delegación)
 * - Manejo de favoritos dinámico
 */

document.addEventListener('DOMContentLoaded', () => {
  initFilters();
  animatePropertyCards();
  initPagination();
  initFavoritos();
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
  const cards = document.querySelectorAll('#gridContainer .property-card');
  if (cards.length === 0) return;

  cards.forEach((card, index) => {
    card.style.opacity = '0';
    card.style.transform = 'translateY(20px)';
    card.style.transition = 'opacity 0.3s ease, transform 0.3s ease';
    
    setTimeout(() => {
      card.style.opacity = '1';
      card.style.transform = 'translateY(0)';
    }, index * 100);
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

    // Reaplicar animaciones a las nuevas tarjetas
    animatePropertyCards();
    // Reinicializar los botones de favoritos
    initFavoritos();
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

/**
 * Inicializa el manejo de favoritos
 */
function initFavoritos() {
  // Delegar eventos en el contenedor para manejar cards nuevas
  const gridContainer = document.getElementById('gridContainer');
  
  gridContainer.addEventListener('click', async (e) => {
    const target = e.target.closest('.btn-favorito');
    if (!target) return;

    e.preventDefault();
    const propiedadId = target.getAttribute('data-propiedad-id');
    const icon = target.querySelector('i');
    const isFavorite = icon.classList.contains('bi-heart-fill');

    try {
      const response = await fetch(`/api/usuario/${isFavorite ? 'eliminar' : 'agregar'}-favorito/${propiedadId}`, {
        method: isFavorite ? 'DELETE' : 'POST',
        headers: {
          'Content-Type': 'application/json',
        }
      });

      if (!response.ok) throw new Error('Error al actualizar favorito');

      const data = await response.json();
      if (data.success) {
        icon.classList.toggle('bi-heart');
        icon.classList.toggle('bi-heart-fill');
        target.classList.toggle('btn-outline-danger');
        target.classList.toggle('btn-danger');
      }
    } catch (error) {
      console.error('Error:', error);
      alert('No se pudo actualizar el estado de favorito. Inténtalo de nuevo.');
    }
  });

  // Cargar el estado inicial de favoritos
  cargarEstadoFavoritos();
}

/**
 * Carga el estado inicial de los favoritos
 */
async function cargarEstadoFavoritos() {
  try {
    const response = await fetch('/api/usuario/propiedades-favoritas');
    if (!response.ok) throw new Error('Error al cargar favoritos');
    
    const favoritos = await response.json();
    const favoritosIds = favoritos.map(p => p.id);
    
    document.querySelectorAll('.btn-favorito').forEach(btn => {
      const propiedadId = parseInt(btn.getAttribute('data-propiedad-id'));
      const icon = btn.querySelector('i');
      
      if (favoritosIds.includes(propiedadId)) {
        icon.classList.remove('bi-heart');
        icon.classList.add('bi-heart-fill');
        btn.classList.remove('btn-outline-danger');
        btn.classList.add('btn-danger');
      }
    });
  } catch (error) {
    console.error('Error al cargar estado de favoritos:', error);
  }
}