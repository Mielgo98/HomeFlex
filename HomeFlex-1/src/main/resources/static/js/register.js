document.addEventListener('DOMContentLoaded', function() {
  const registerForm = document.getElementById('registerForm');
  if (registerForm) {
    registerForm.addEventListener('submit', function(e) {
      const password = document.getElementById('password');
      const confirmPassword = document.getElementById('confirmPassword');
      
      if (password && confirmPassword) {
        if (password.value.length < 8) {
          e.preventDefault();
          alert('La contraseña debe tener al menos 8 caracteres.');
          return;
        }
        
        if (password.value !== confirmPassword.value) {
          e.preventDefault();
          alert('Las contraseñas no coinciden.');
        }
      }
    });
  }
});