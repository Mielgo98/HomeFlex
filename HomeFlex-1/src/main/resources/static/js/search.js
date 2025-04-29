/**
 * HomeFlex - Script para funcionalidades de búsqueda
 * 
 * Este script maneja:
 * 1. Validación de fechas en el formulario de búsqueda
 * 2. Inicialización de fechas mínimas
 * 3. Actualización dinámica de la fecha mínima de salida
 */

document.addEventListener('DOMContentLoaded', function() {
  // Referencia al formulario de búsqueda
  const searchForm = document.getElementById('searchForm');
  
  // Validación de fechas en el formulario
  if (searchForm) {
    searchForm.addEventListener('submit', function(e) {
      const fechaInicio = document.querySelector('input[name="fechaInicio"]');
      const fechaFin = document.querySelector('input[name="fechaFin"]');
      
      if (fechaInicio && fechaFin && fechaInicio.value && fechaFin.value) {
        const inDate = new Date(fechaInicio.value);
        const outDate = new Date(fechaFin.value);
        
        if (outDate <= inDate) {
          e.preventDefault();
          alert('La fecha de salida debe ser posterior a la de entrada.');
        }
      }
    });
  }
  
  // Inicialización de fechas mínimas
  initializeDatePickers();
});

/**
 * Inicializa los selectores de fecha estableciendo valores mínimos
 * y manejando la actualización dinámica de fechas
 */
function initializeDatePickers() {
  // Establecer fecha mínima como hoy para el input de fecha de entrada
  const today = new Date().toISOString().split('T')[0];
  const fechaInicio = document.querySelector('input[name="fechaInicio"]');
  const fechaFin = document.querySelector('input[name="fechaFin"]');
  
  if (fechaInicio) {
    fechaInicio.min = today;
    
    // Establecer fecha de salida mínima al cambiar fecha de entrada
    fechaInicio.addEventListener('change', function() {
      if (fechaFin) {
        // La fecha de salida debe ser al menos un día después de la fecha de entrada
        const nextDay = new Date(fechaInicio.value);
        nextDay.setDate(nextDay.getDate() + 1);
        fechaFin.min = nextDay.toISOString().split('T')[0];
        
        // Si la fecha de salida es anterior a la nueva fecha mínima, actualizarla
        if (fechaFin.value && new Date(fechaFin.value) < nextDay) {
          fechaFin.value = nextDay.toISOString().split('T')[0];
        }
      }
    });
  }
}

/**
 * Función auxiliar para establecer una ciudad en el campo de búsqueda
 * @param {string} ciudad - Nombre de la ciudad a establecer
 */
function seleccionarCiudad(ciudad) {
  document.querySelector('input[name="ciudad"]').value = ciudad;
  return false;
}