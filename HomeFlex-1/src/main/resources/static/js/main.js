document.addEventListener('DOMContentLoaded', function() {
  console.log('HomeFlex app initialized');
  
  // Inicializar tooltips si existe la función
  if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip) {
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
      return new bootstrap.Tooltip(tooltipTriggerEl);
    });
  }
  
  // Inicializar manejo de menú móvil
  const menuBtn = document.querySelector('.navbar-toggler');
  if (menuBtn) {
    menuBtn.addEventListener('click', function() {
      console.log('Menu button clicked');
    });
  }
});