document.addEventListener('DOMContentLoaded', () => {
  // Buscamos el form por el atributo 'action', no por 'th:action'
  const filtroForm = document.querySelector('form[action*="/reservas/mis-reservas"]');
  if (!filtroForm) {
    console.warn('No se encontró el formulario de filtros de reservas');
    return;
  }

  const estadoSelect = filtroForm.querySelector('#estado');
  const busquedaInput = filtroForm.querySelector('#busqueda');

  // Auto-submit al cambiar estado o al pulsar Enter en el input de búsqueda
  estadoSelect.addEventListener('change', () => filtroForm.submit());
  busquedaInput.addEventListener('keydown', e => {
    if (e.key === 'Enter') {
      e.preventDefault();
      filtroForm.submit();
    }
  });

  // Smooth scroll al hacer clic en enlaces de paginación
  document.querySelectorAll('.pagination .page-link').forEach(link => {
    link.addEventListener('click', () => {
      // dejamos que el navegador navegue primero
      setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }), 50);
    });
  });
});
