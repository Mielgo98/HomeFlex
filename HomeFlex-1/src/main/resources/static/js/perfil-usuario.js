// perfil-usuario.js

document.addEventListener('DOMContentLoaded', () => {
  // 1) Alternar visibilidad de campos de contraseña
  document.querySelectorAll('.toggle-password').forEach(btn => {
    btn.addEventListener('click', () => {
      const targetId = btn.getAttribute('data-target');
      const input = document.getElementById(targetId);
      const icon  = btn.querySelector('i');
      if (input.type === 'password') {
        input.type = 'text';
        icon.classList.replace('bi-eye', 'bi-eye-slash');
      } else {
        input.type = 'password';
        icon.classList.replace('bi-eye-slash', 'bi-eye');
      }
    });
  });

  // 2) Función para evaluar la fortaleza de la nueva contraseña
  function evaluatePasswordStrength(pwd) {
    let score = 0;
    if (pwd.length >= 8)          score += 25;
    if (/[A-Z]/.test(pwd))        score += 25;
    if (/[a-z]/.test(pwd))        score += 25;
    if (/\d/.test(pwd))           score += 15;
    if (/[^A-Za-z0-9]/.test(pwd)) score += 10;
    return score;
  }

  // 3) Conexión del medidor de fortaleza
  const meter = document.getElementById('password-strength-meter');
  const text  = document.getElementById('password-strength-text');
  const pwdIn = document.getElementById('passwordNueva');

  pwdIn.addEventListener('input', e => {
    const strength = evaluatePasswordStrength(e.target.value);
    meter.style.width = strength + '%';
    meter.setAttribute('aria-valuenow', strength);

    // Remover clases previas
    meter.classList.remove('bg-danger', 'bg-warning', 'bg-success');

    // Asignar nueva clase y texto descriptivo
    if (strength < 50) {
      meter.classList.add('bg-danger');
      text.textContent = 'Contraseña débil';
    } else if (strength < 75) {
      meter.classList.add('bg-warning');
      text.textContent = 'Contraseña moderada';
    } else {
      meter.classList.add('bg-success');
      text.textContent = 'Contraseña fuerte';
    }
  });
});

    