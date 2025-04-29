document.addEventListener('DOMContentLoaded', function() {
  const loginForm = document.querySelector('form[action="/login"]');
  if (loginForm) {
    loginForm.addEventListener('submit', function(e) {
      const username = document.getElementById('username');
      const password = document.getElementById('password');
      
      if (username && password) {
        if (!username.value.trim() || !password.value.trim()) {
          e.preventDefault();
          alert('Por favor, completa todos los campos.');
        }
      }
    });
  }
});