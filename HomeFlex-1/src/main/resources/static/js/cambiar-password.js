// Función para alternar visibilidad de campos de contraseña
   document.querySelectorAll('.toggle-password').forEach(btn => {
     btn.addEventListener('click', () => {
       const targetId = btn.getAttribute('data-target');
       const input = document.getElementById(targetId);
       const icon = btn.querySelector('i');
       if (input.type === 'password') {
         input.type = 'text';
         icon.classList.replace('bi-eye', 'bi-eye-slash');
       } else {
         input.type = 'password';
         icon.classList.replace('bi-eye-slash', 'bi-eye');
       }
     });
   });

   // Función para evaluar fortaleza de la nueva contraseña
   function evaluatePasswordStrength(pwd) {
     let score = 0;
     if (pwd.length >= 8) score += 25;
     if (/[A-Z]/.test(pwd)) score += 25;
     if (/[a-z]/.test(pwd)) score += 25;
     if (/\d/.test(pwd)) score += 15;
     if (/[^A-Za-z0-9]/.test(pwd)) score += 10;
     return score;
   }

   // Actualizar medidor de fortaleza
   const meter = document.getElementById('password-strength-meter');
   const text = document.getElementById('password-strength-text');
   document.getElementById('passwordNueva').addEventListener('input', e => {
     const val = e.target.value;
     const strength = evaluatePasswordStrength(val);
     meter.style.width = strength + '%';
     meter.setAttribute('aria-valuenow', strength);
     if (strength < 50) {
       meter.className = 'progress-bar bg-danger';
       text.textContent = 'Contraseña débil';
     } else if (strength < 75) {
       meter.className = 'progress-bar bg-warning';
       text.textContent = 'Contraseña moderada';
     } else {
       meter.className = 'progress-bar bg-success';
       text.textContent = 'Contraseña fuerte';
     }
   });