/**
 * JS para la página de listado de propiedades de HomeFlex
 * Funcionalidades:
 * 1. Navegación y filtrado de propiedades
 * 2. Manejo de filtros de búsqueda
 */

document.addEventListener('DOMContentLoaded', () => {
  // Inicializar manejo de filtros
  initFilters();
  
  // Aplicar animaciones de entrada a las tarjetas de propiedades
  animatePropertyCards();
  
  // Inicializar paginación dinámica 
  initPagination();
});

/**
 * Inicializa el manejo de filtros y su comportamiento
 */
function initFilters() {
  // Obtener formulario de filtros
  const filtersForm = document.getElementById('filtersForm');
  if (!filtersForm) return;
  
  // Filtrado automático al cambiar ordenación
  const ordenarSelect = document.getElementById('ordenar');
  if (ordenarSelect) {
    ordenarSelect.addEventListener('change', () => {
      filtersForm.submit();
    });
  }
  
  // Sugerencias de ciudades populares
  const ciudadInput = document.getElementById('ciudad');
  const popularCities = document.querySelectorAll('.popular-cities a');
  
  if (ciudadInput && popularCities.length > 0) {
    popularCities.forEach(city => {
      city.addEventListener('click', (e) => {
        e.preventDefault();
        ciudadInput.value = city.textContent.trim();
        filtersForm.submit();
      });
    });
    
    // Enviar formulario al presionar Enter en el input de ciudad
    ciudadInput.addEventListener('keypress', (e) => {
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
      // Limpiar todos los campos del formulario
      filtersForm.reset();
      
      // Redireccionar a la página principal de propiedades
      window.location.href = '/propiedades';
    });
  }
}

/**
 * Aplica animaciones de entrada a las tarjetas de propiedades
 * para crear un efecto visual atractivo
 */
function animatePropertyCards() {
  const propertyCards = document.querySelectorAll('.property-card');
  
  if (propertyCards.length === 0) return;
  
  // Aplicar un retraso incremental a cada tarjeta para efecto escalonado
  propertyCards.forEach((card, index) => {
    card.style.opacity = '0';
    card.style.animationDelay = `${index * 0.1}s`;
    
    // Observar cuando la tarjeta entre en el viewport
    const observer = new IntersectionObserver((entries) => {
      if (entries[0].isIntersecting) {
        card.style.opacity = '1';
        observer.unobserve(card);
      }
    }, {
      threshold: 0.1
    });
    
    observer.observe(card);
  });
}

/**
 * Inicializa la paginación dinámica y su comportamiento
 */
function initPagination() {
  const paginationLinks = document.querySelectorAll('.pagination .page-link');
  if (paginationLinks.length === 0) return;
  
  paginationLinks.forEach(link => {
    link.addEventListener('click', (e) => {
      // No interrumpir navegación para enlaces deshabilitados
      if (link.parentElement.classList.contains('disabled')) {
        e.preventDefault();
        return;
      }
      
      // Añadir indicador de carga
      document.querySelector('.properties-container').classList.add('loading');
      
      // Permitir que la navegación continúe normalmente
    });
  });
  
  // Restaurar posición de scroll cuando se navega con el botón Atrás
  window.addEventListener('pageshow', (event) => {
    if (event.persisted) {
      // La página se carga desde el caché del navegador (botón Atrás)
      setTimeout(() => {
        window.scrollTo(0, sessionStorage.getItem('scrollPosition') || 0);
      }, 100);
    }
  });
  
  // Guardar posición de scroll antes de cambiar de página
  window.addEventListener('beforeunload', () => {
    sessionStorage.setItem('scrollPosition', window.scrollY);
  });
}