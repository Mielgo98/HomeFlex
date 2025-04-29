/**
 * Script de validación mejorado para el formulario de registro
 * Muestra todos los errores de validación al intentar enviar el formulario
 */

document.addEventListener('DOMContentLoaded', function() {
  const registerForm = document.getElementById('registerForm');
  if (!registerForm) return;
  
  // Referencias a los campos del formulario
  const nombre = document.getElementById('nombre');
  const apellidos = document.getElementById('apellidos');
  const email = document.getElementById('email');
  const username = document.getElementById('username');
  const telefono = document.getElementById('telefono');
  const password = document.getElementById('password');
  const confirmPassword = document.getElementById('confirmPassword');
  const aceptaTerminos = document.getElementById('aceptaTerminos');
  
  // Patrones de validación
  const patterns = {
    username: /^[a-zA-Z0-9._-]{4,50}$/,
    email: /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/,
    password: /^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\S+$).{8,}$/,
    telefono: /^(6|7|8|9)\d{8}$/
  };
  
  // Mensajes de error
  const errorMessages = {
    required: 'Este campo es obligatorio',
    nombre: 'El nombre debe tener entre 2 y 50 caracteres',
    apellidos: 'Los apellidos deben tener entre 2 y 100 caracteres',
    username: 'El nombre de usuario debe tener entre 4 y 50 caracteres y solo puede contener letras, números, puntos, guiones y guiones bajos',
    email: 'Introduce una dirección de correo electrónico válida',
    password: 'La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial',
    confirmPassword: 'Las contraseñas no coinciden',
    telefono: 'El teléfono debe comenzar con 6, 7, 8 o 9, seguido de 8 dígitos',
    terms: 'Debes aceptar los términos y condiciones para continuar'
  };
  
  // Función para mostrar error en un campo
  function showError(field, message) {
    field.classList.add('is-invalid');
    
    // Buscar o crear el div de feedback
    let feedback = field.nextElementSibling;
    if (!feedback || !feedback.classList.contains('invalid-feedback')) {
      feedback = document.createElement('div');
      feedback.className = 'invalid-feedback';
      field.parentNode.insertBefore(feedback, field.nextSibling);
    }
    
    feedback.style.display = 'block';
    feedback.textContent = message;
  }
  
  // Función para quitar error de un campo
  function clearError(field) {
    field.classList.remove('is-invalid');
    
    const feedback = field.nextElementSibling;
    if (feedback && feedback.classList.contains('invalid-feedback')) {
      feedback.style.display = 'none';
    }
  }
  
  // Validar campo requerido
  function validateRequired(field, fieldName) {
    if (!field.value.trim()) {
      showError(field, errorMessages.required);
      return false;
    }
    return true;
  }
  
  // Validar campo con patrón
  function validatePattern(field, pattern, errorMessage) {
    if (!field.value.trim()) return false; // Si está vacío, ya se validó con required
    
    if (!pattern.test(field.value)) {
      showError(field, errorMessage);
      return false;
    }
    return true;
  }
  
  // Validar longitud
  function validateLength(field, min, max, errorMessage) {
    if (!field.value.trim()) return false; // Si está vacío, ya se validó con required
    
    const length = field.value.trim().length;
    if (length < min || length > max) {
      showError(field, errorMessage);
      return false;
    }
    return true;
  }
  
  // Validar coincidencia de contraseñas
  function validatePasswordMatch() {
    if (!confirmPassword.value.trim()) return false; // Si está vacío, ya se validó con required
    
    if (password.value !== confirmPassword.value) {
      showError(confirmPassword, errorMessages.confirmPassword);
      return false;
    }
    return true;
  }
  
  // Validar términos y condiciones
  function validateTerms() {
    if (!aceptaTerminos.checked) {
      aceptaTerminos.classList.add('is-invalid');
      let feedback = aceptaTerminos.parentNode.querySelector('.invalid-feedback');
      if (feedback) {
        feedback.style.display = 'block';
      } else {
        feedback = document.createElement('div');
        feedback.className = 'invalid-feedback';
        feedback.textContent = errorMessages.terms;
        feedback.style.display = 'block';
        aceptaTerminos.parentNode.appendChild(feedback);
      }
      return false;
    }
    
    aceptaTerminos.classList.remove('is-invalid');
    const feedback = aceptaTerminos.parentNode.querySelector('.invalid-feedback');
    if (feedback) {
      feedback.style.display = 'none';
    }
    return true;
  }
  
  // Validar todo el formulario
  function validateForm() {
    let isValid = true;
    
    // Limpiar todos los errores anteriores
    document.querySelectorAll('.is-invalid').forEach(field => {
      field.classList.remove('is-invalid');
    });
    
    document.querySelectorAll('.invalid-feedback').forEach(feedback => {
      feedback.style.display = 'none';
    });
    
    // Validar campos obligatorios
    if (!validateRequired(nombre, 'nombre')) isValid = false;
    if (!validateRequired(apellidos, 'apellidos')) isValid = false;
    if (!validateRequired(email, 'email')) isValid = false;
    if (!validateRequired(username, 'username')) isValid = false;
    if (!validateRequired(telefono, 'telefono')) isValid = false;
    if (!validateRequired(password, 'password')) isValid = false;
    if (!validateRequired(confirmPassword, 'confirmPassword')) isValid = false;
    
    // Validar formatos
    if (nombre.value.trim() && !validateLength(nombre, 2, 50, errorMessages.nombre)) isValid = false;
    if (apellidos.value.trim() && !validateLength(apellidos, 2, 100, errorMessages.apellidos)) isValid = false;
    if (email.value.trim() && !validatePattern(email, patterns.email, errorMessages.email)) isValid = false;
    if (username.value.trim() && !validatePattern(username, patterns.username, errorMessages.username)) isValid = false;
    if (telefono.value.trim() && !validatePattern(telefono, patterns.telefono, errorMessages.telefono)) isValid = false;
    if (password.value.trim() && !validatePattern(password, patterns.password, errorMessages.password)) isValid = false;
    
    // Validar coincidencia de contraseñas
    if (confirmPassword.value.trim() && !validatePasswordMatch()) isValid = false;
    
    // Validar términos
    if (!validateTerms()) isValid = false;
    
    return isValid;
  }
  
  // Evento al enviar el formulario
  registerForm.addEventListener('submit', function(event) {
    if (!validateForm()) {
      event.preventDefault();
    }
  });
  
  // Limpiar errores al cambiar los campos
  const inputFields = [nombre, apellidos, email, username, telefono, password, confirmPassword];
  inputFields.forEach(field => {
    field.addEventListener('input', function() {
      clearError(this);
    });
  });
  
  // Para términos y condiciones
  aceptaTerminos.addEventListener('change', function() {
    if (this.checked) {
      this.classList.remove('is-invalid');
      const feedback = this.parentNode.querySelector('.invalid-feedback');
      if (feedback) {
        feedback.style.display = 'none';
      }
    }
  });
  
  // Validar coincidencia de contraseñas mientras se escribe
  confirmPassword.addEventListener('input', function() {
    if (this.value && password.value && this.value !== password.value) {
      showError(this, errorMessages.confirmPassword);
    }
  });
  
  password.addEventListener('input', function() {
    if (confirmPassword.value && this.value && confirmPassword.value !== this.value) {
      showError(confirmPassword, errorMessages.confirmPassword);
    } else if (confirmPassword.value) {
      clearError(confirmPassword);
    }
  });
});