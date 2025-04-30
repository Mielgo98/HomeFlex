
document.addEventListener('DOMContentLoaded', () => {
  const filtroForm = document.querySelector('form[th\\:action*="/reservas/mis-reservas"]');
  const estadoSelect = filtroForm.querySelector('#estado');
  const busquedaInput = filtroForm.querySelector('#busqueda');

  // Auto-submit al cambiar estado o al borrar/añadir texto y pulsar Enter
  estadoSelect.addEventListener('change', () => filtroForm.submit());
  busquedaInput.addEventListener('keydown', e => {
    if (e.key === 'Enter') {
      e.preventDefault();
      filtroForm.submit();
    }
  });

  // Smooth scroll al hacer clic en enlaces de paginación
  document.querySelectorAll('.pagination .page-link').forEach(link => {
    link.addEventListener('click', (e) => {
      // deja que el navegador navegue primero
      setTimeout(() => window.scrollTo({ top: 0, behavior: 'smooth' }), 50);
    });
  });
});
