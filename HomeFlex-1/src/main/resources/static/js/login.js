document.querySelector('form[th\:action="@{/login}"]').addEventListener('submit', e => {
  const username = document.getElementById('username').value.trim();
  const password = document.getElementById('password').value.trim();
  if (!username || !password) {
    e.preventDefault();
    alert('Por favor, completa usuario y contraseña.');
  }
});