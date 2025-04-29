document.getElementById('registerForm').addEventListener('submit', e => {
  const email = document.getElementById('email').value.trim();
  const user = document.getElementById('usernameReg').value.trim();
  const pass = document.getElementById('passwordReg').value;
  const confirm = document.getElementById('confirmPassword').value;
  if (!email || !user || !pass || !confirm) {
    e.preventDefault();
    alert('Todos los campos son obligatorios.');
    return;
  }
  if (pass.length < 8) {
    e.preventDefault();
    alert('La contraseña debe tener al menos 8 caracteres.');
    return;
  }
  if (pass !== confirm) {
    e.preventDefault();
    alert('Las contraseñas no coinciden.');
  }
});