/**
 * Script de validación para el formulario de login
 * Muestra errores de validación en tiempo real
 */

document.addEventListener('DOMContentLoaded', function() {
  const loginForm = document.getElementById('loginForm');
  if (!loginForm) return;
  
  // Referencias a los campos del formulario
  const username = document.getElementById('username');
  const password = document.getElementById('password');
  
  // Función para mostrar error en un campo
  function showError(field, message) {
    field.classList.add('is-invalid');
    
    // Buscar el div de feedback
    let feedback = field.nextElementSibling;
    if (feedback && feedback.classList.contains('invalid-feedback')) {
      feedback.style.display = 'block';
      feedback.textContent = message;
    }
  }
  
  // Función para quitar error de un campo
  function clearError(field) {
    field.classList.remove('is-invalid');
    
    const feedback = field.nextElementSibling;
    if (feedback && feedback.classList.contains('invalid-feedback')) {
      feedback.style.display = 'none';
    }
  }
  
  // Evento al enviar el formulario
  loginForm.addEventListener('submit', function(event) {
    let isValid = true;
    
    // Validar username
    if (!username.value.trim()) {
      showError(username, 'El nombre de usuario es obligatorio');
      isValid = false;
    } else {
      clearError(username);
    }
    
    // Validar password
    if (!password.value) {
      showError(password, 'La contraseña es obligatoria');
      isValid = false;
    } else {
      clearError(password);
    }
    
    // Si hay errores, prevenir el envío del formulario
    if (!isValid) {
      event.preventDefault();
      
      // Opcionalmente, mostrar un mensaje de error general
      const existingAlert = document.querySelector('.alert-danger');
      if (!existingAlert) {
        const errorDiv = document.createElement('div');
        errorDiv.className = 'alert alert-danger';
        errorDiv.textContent = 'Por favor, completa todos los campos requeridos.';
        loginForm.prepend(errorDiv);
      }
    }
  });
  
  // Limpiar errores al escribir en los campos
  username.addEventListener('input', function() {
    clearError(this);
    removeErrorAlert();
  });
  
  password.addEventListener('input', function() {
    clearError(this);
    removeErrorAlert();
  });
  
  // Función para eliminar mensaje de error general
  function removeErrorAlert() {
    const alertElement = document.querySelector('.alert-danger:not([th\\:if])');
    if (alertElement) {
      alertElement.remove();
    }
  }
  
  // Manejar el enlace de "Olvidaste tu contraseña"
  const forgotPasswordLink = document.getElementById('forgot-password');
  if (forgotPasswordLink) {
    forgotPasswordLink.addEventListener('click', function(e) {
      e.preventDefault();
      
    
      
      // De momento, mostramos un mensaje informativo
      alert('La funcionalidad de recuperación de contraseña estará disponible próximamente.');
    });
  }
});