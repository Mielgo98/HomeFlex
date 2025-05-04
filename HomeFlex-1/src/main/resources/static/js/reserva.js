/**
 * HomeFlex - Script para gestión de reservas y pagos
 * Implementado con SweetAlert2 para las notificaciones y confirmaciones
 * Incluye verificación de JWT
 */
document.addEventListener('DOMContentLoaded', () => {
  // Obtiene el valor de una cookie
  function getCookie(name) {
    const matches = document.cookie.match(new RegExp(
      "(?:^|; )" +
      name.replace(/([.$?*|{}()[\]\\\/+^])/g, '\\$1') +
      "=([^;]*)"
    ));
    return matches ? decodeURIComponent(matches[1]) : null;
  }
  
  const token = getCookie('jwt_token');
  
  // Gestión de filtros de reservas
  const filtroForm = document.querySelector('form[action*="/reservas/mis-reservas"]');
  if (filtroForm) {
    const estadoSelect = filtroForm.querySelector('#estado');
    const busquedaInput = filtroForm.querySelector('#busqueda');

    if (estadoSelect) {
      estadoSelect.addEventListener('change', () => filtroForm.submit());
    }
    
    if (busquedaInput) {
      busquedaInput.addEventListener('keydown', e => {
        if (e.key === 'Enter') {
          e.preventDefault();
          filtroForm.submit();
        }
      });
    }
  }

  // GESTIÓN DE FORMULARIO DE PAGO
  
  // Formulario de pago
  const paymentForm = document.getElementById('paymentForm');
  if (paymentForm) {
    const numeroTarjeta = document.getElementById('numeroTarjeta');
    const fechaExpiracion = document.getElementById('fechaExpiracion');
    const cvv = document.getElementById('cvv');
    const titular = document.getElementById('titular');
    
    // Formatear número de tarjeta
    if (numeroTarjeta) {
      numeroTarjeta.addEventListener('input', function(e) {
        let value = e.target.value.replace(/\s+/g, '').replace(/[^0-9]/gi, '');
        let formattedValue = '';
        
        for (let i = 0; i < value.length; i++) {
          if (i > 0 && i % 4 === 0) {
            formattedValue += ' ';
          }
          formattedValue += value[i];
        }
        
        e.target.value = formattedValue;
      });
    }
    
    // Formatear fecha de expiración
    if (fechaExpiracion) {
      fechaExpiracion.addEventListener('input', function(e) {
        let value = e.target.value.replace(/[^0-9]/gi, '');
        
        if (value.length > 0) {
          if (value.length <= 2) {
            e.target.value = value;
          } else {
            e.target.value = value.substring(0, 2) + '/' + value.substring(2, 4);
          }
          
          // Validar mes (01-12)
          if (value.length >= 2) {
            let month = parseInt(value.substring(0, 2));
            if (month < 1 || month > 12) {
              fechaExpiracion.classList.add('is-invalid');
            } else {
              fechaExpiracion.classList.remove('is-invalid');
            }
          }
        }
      });
    }
    
    // Permitir solo números en CVV
    if (cvv) {
      cvv.addEventListener('input', function(e) {
        e.target.value = e.target.value.replace(/[^0-9]/gi, '');
      });
    }
    
    // Validar formulario antes de enviar
    paymentForm.addEventListener('submit', function(e) {
      e.preventDefault();
      
      let isValid = true;
      
      // Validar número de tarjeta
      if (numeroTarjeta && !validarNumeroTarjeta(numeroTarjeta.value.replace(/\s+/g, ''))) {
        numeroTarjeta.classList.add('is-invalid');
        isValid = false;
      } else if (numeroTarjeta) {
        numeroTarjeta.classList.remove('is-invalid');
      }
      
      // Validar fecha de expiración
      if (fechaExpiracion && !validarFechaExpiracion(fechaExpiracion.value)) {
        fechaExpiracion.classList.add('is-invalid');
        isValid = false;
      } else if (fechaExpiracion) {
        fechaExpiracion.classList.remove('is-invalid');
      }
      
      // Validar CVV
      if (cvv && !/^\d{3,4}$/.test(cvv.value)) {
        cvv.classList.add('is-invalid');
        isValid = false;
      } else if (cvv) {
        cvv.classList.remove('is-invalid');
      }
      
      // Validar titular
      if (titular && titular.value.trim().length < 3) {
        titular.classList.add('is-invalid');
        isValid = false;
      } else if (titular) {
        titular.classList.remove('is-invalid');
      }
      
      if (!isValid) {
        Swal.fire({
          icon: 'error',
          title: 'Datos incorrectos',
          text: 'Por favor, verifica los datos de tu tarjeta',
          confirmButtonText: 'Entendido'
        });
        return;
      }
      
      // Mostrar indicador de procesamiento
      Swal.fire({
        title: 'Procesando pago',
        html: 'Por favor, espera mientras procesamos tu pago...',
        allowOutsideClick: false,
        didOpen: () => {
          Swal.showLoading();
        }
      });
      
      // Enviar formulario con AJAX
      const formData = new FormData(paymentForm);
      
      fetch(paymentForm.action, {
        method: 'POST',
        body: formData,
        headers: {
          'X-Requested-With': 'XMLHttpRequest'
        }
      })
      .then(response => {
        // Verificar si la respuesta es JSON
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('application/json')) {
          return response.json();
        }
        
        // Si no es JSON, puede ser una redirección o un error
        if (response.redirected) {
          window.location.href = response.url;
          throw new Error('Redirección');
        }
        
        if (response.status === 401) {
          window.location.href = '/login';
          throw new Error('No autorizado');
        }
        
        throw new Error(`Error inesperado: ${response.status}`);
      })
      .then(data => {
        if (data.success) {
          // Mostrar mensaje de éxito
          Swal.fire({
            icon: 'success',
            title: '¡Pago completado!',
            text: data.message || 'El pago se ha realizado correctamente',
            confirmButtonText: 'Ver mi reserva'
          }).then((result) => {
            if (result.isConfirmed) {
              window.location.href = `/reservas/${data.reservaId}`;
            }
          });
        } else {
          // Mostrar mensaje de error
          Swal.fire({
            icon: 'error',
            title: 'Error en el pago',
            text: data.message || 'Ha ocurrido un error al procesar el pago',
            confirmButtonText: 'Intentar nuevamente'
          });
        }
      })
      .catch(err => {
        console.error('Error:', err);
        
        // No mostrar mensaje para redirecciones intencionales
        if (err.message === 'Redirección') {
          return;
        }
        
        Swal.fire({
          icon: 'error',
          title: 'Error de conexión',
          text: 'Ha ocurrido un problema con el servidor. Por favor, intenta nuevamente.',
          confirmButtonText: 'Entendido'
        });
      });
    });
  }
  
  // BOTONES DE PAGO DIRECTO
  const botonesInicioPago = document.querySelectorAll('a[href*="/pago/iniciar"]');
  botonesInicioPago.forEach(boton => {
    boton.addEventListener('click', function(e) {
      e.preventDefault();
      
      // Mostrar SweetAlert de confirmación
      Swal.fire({
        title: '¿Realizar pago?',
        text: 'Estás a punto de realizar el pago de esta reserva',
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: 'Sí, pagar ahora',
        cancelButtonText: 'Cancelar',
        reverseButtons: true
      }).then((result) => {
        if (result.isConfirmed) {
          // Mostrar indicador de procesamiento
          Swal.fire({
            title: 'Iniciando proceso de pago',
            html: 'Te estamos redirigiendo a la pasarela de pago...',
            allowOutsideClick: false,
            didOpen: () => {
              Swal.showLoading();
            },
            timer: 1500
          }).then(() => {
            // Usar redirección directa en lugar de AJAX
            const url = boton.getAttribute('href');
            window.location.href = url;
          });
        }
      });
    });
  });
  
  // Simulador de pago
  const simulatorForm = document.querySelector('form.simulator');
  if (simulatorForm) {
    simulatorForm.addEventListener('submit', function(e) {
      e.preventDefault();
      
      // Mostrar indicador de procesamiento
      Swal.fire({
        title: 'Procesando pago simulado',
        html: 'Por favor, espera mientras procesamos tu pago...',
        allowOutsideClick: false,
        didOpen: () => {
          Swal.showLoading();
        },
        timer: 2000
      }).then(() => {
        // Después de la simulación, verificar el estado
        const idSesion = simulatorForm.querySelector('input[name="idSesion"]').value;
        
        // Verificar el estado del pago
        fetch(`/pago/verificar?idSesion=${idSesion}`, {
          method: 'GET',
          headers: {
            'X-Requested-With': 'XMLHttpRequest'
          }
        })
        .then(response => {
          // Verificar si la respuesta es JSON
          const contentType = response.headers.get('content-type');
          if (contentType && contentType.includes('application/json')) {
            return response.json();
          }
          
          // Si no es JSON, puede ser una redirección o un error
          if (response.redirected) {
            window.location.href = response.url;
            throw new Error('Redirección');
          }
          
          if (response.status === 401) {
            window.location.href = '/login';
            throw new Error('No autorizado');
          }
          
          throw new Error(`Error inesperado: ${response.status}`);
        })
        .then(data => {
          if (data.success) {
            // Mostrar mensaje de éxito
            Swal.fire({
              icon: 'success',
              title: '¡Pago completado!',
              html: `
                <p>Tu reserva <strong>${data.reserva.codigoReserva}</strong> ha sido pagada correctamente.</p>
                <p>Importe: ${data.pago.monto} ${data.pago.moneda}</p>
              `,
              confirmButtonText: 'Ver mi reserva'
            }).then((result) => {
              if (result.isConfirmed) {
                window.location.href = `/reservas/${data.reserva.id}`;
              }
            });
          } else {
            // Mostrar mensaje de error
            Swal.fire({
              icon: 'error',
              title: 'Error en el pago',
              text: data.message || 'Ha ocurrido un error al procesar el pago',
              confirmButtonText: 'Intentar nuevamente'
            });
          }
        })
        .catch(err => {
          console.error('Error:', err);
          
          // No mostrar mensaje para redirecciones intencionales
          if (err.message === 'Redirección') {
            return;
          }
          
          Swal.fire({
            icon: 'error',
            title: 'Error de conexión',
            text: 'Ha ocurrido un problema con el servidor. Por favor, intenta nuevamente.',
            confirmButtonText: 'Entendido'
          });
        });
      });
    });
  }
  
  // GESTIÓN DE ACCIONES DE RESERVA
  
  // Botones de acción para reservas (aprobar, rechazar, cancelar)
  const reservaActionButtons = document.querySelectorAll('[data-reserva-action]');
  reservaActionButtons.forEach(button => {
    button.addEventListener('click', function(e) {
      e.preventDefault();
      
      const action = this.getAttribute('data-reserva-action');
      const reservaId = this.getAttribute('data-reserva-id');
      const url = `/api/reservas/${reservaId}/${action}`;
      
      let title = '';
      let text = '';
      let inputOptions = {};
      
      // Configurar mensaje de confirmación según la acción
      switch(action) {
        case 'aprobar':
          title = '¿Aprobar esta reserva?';
          text = 'Esta acción cambiará el estado a PENDIENTE_PAGO';
          break;
        case 'rechazar':
          title = '¿Rechazar esta reserva?';
          text = 'Por favor, indica el motivo del rechazo';
          inputOptions = {
            input: 'text',
            inputPlaceholder: 'Motivo del rechazo',
            inputValidator: (value) => {
              if (!value) {
                return 'Debes indicar un motivo';
              }
            }
          };
          break;
        case 'cancelar':
          title = '¿Cancelar esta reserva?';
          text = 'Por favor, indica el motivo de la cancelación';
          inputOptions = {
            input: 'text',
            inputPlaceholder: 'Motivo de la cancelación',
            inputValidator: (value) => {
              if (!value) {
                return 'Debes indicar un motivo';
              }
            }
          };
          break;
        default:
          title = '¿Confirmar esta acción?';
          text = 'Esta acción no se puede deshacer';
          break;
      }
      
      // Mostrar el diálogo de confirmación
      Swal.fire({
        title: title,
        text: text,
        icon: 'question',
        showCancelButton: true,
        confirmButtonText: 'Sí, confirmar',
        cancelButtonText: 'Cancelar',
        reverseButtons: true,
        ...inputOptions
      }).then((result) => {
        if (result.isConfirmed) {
          // Mostrar indicador de procesamiento
          Swal.fire({
            title: 'Procesando...',
            html: 'Por favor, espera mientras procesamos tu solicitud...',
            allowOutsideClick: false,
            didOpen: () => {
              Swal.showLoading();
            }
          });
          
          // Crear los datos para la solicitud
          const formData = new FormData();
          if (result.value && inputOptions.input) {
            formData.append('motivo', result.value);
          }
          
          // Realizar la solicitud
          fetch(url, {
            method: 'POST',
            body: formData,
            headers: {
              'X-Requested-With': 'XMLHttpRequest'
            }
          })
          .then(response => {
            // Verificar si la respuesta es JSON
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
              return response.json();
            }
            
            // Si no es JSON, puede ser una redirección o un error
            if (response.redirected) {
              window.location.href = response.url;
              throw new Error('Redirección');
            }
            
            if (response.status === 401) {
              window.location.href = '/login';
              throw new Error('No autorizado');
            }
            
            throw new Error(`Error inesperado: ${response.status}`);
          })
          .then(data => {
            if (data.success) {
              // Mostrar mensaje de éxito
              Swal.fire({
                icon: 'success',
                title: 'Operación completada',
                text: data.message || 'La operación se completó con éxito',
                confirmButtonText: 'Aceptar'
              }).then(() => {
                // Recargar la página
                window.location.reload();
              });
            } else {
              // Mostrar mensaje de error
              Swal.fire({
                icon: 'error',
                title: 'Error',
                text: data.message || 'Ha ocurrido un error al procesar la solicitud',
                confirmButtonText: 'Entendido'
              });
            }
          })
          .catch(err => {
            console.error('Error:', err);
            
            // No mostrar mensaje para redirecciones intencionales
            if (err.message === 'Redirección') {
              return;
            }
            
            Swal.fire({
              icon: 'error',
              title: 'Error de conexión',
              text: 'Ha ocurrido un problema con el servidor. Por favor, intenta nuevamente.',
              confirmButtonText: 'Entendido'
            });
          });
        }
      });
    });
  });
  
  // FUNCIONES AUXILIARES
  
  // Función para validar número de tarjeta (algoritmo Luhn)
  function validarNumeroTarjeta(numero) {
    if (!/^\d{13,19}$/.test(numero)) return false;
    
    let sum = 0;
    let shouldDouble = false;
    
    // Algoritmo de Luhn
    for (let i = numero.length - 1; i >= 0; i--) {
      let digit = parseInt(numero.charAt(i));
      
      if (shouldDouble) {
        digit *= 2;
        if (digit > 9) digit -= 9;
      }
      
      sum += digit;
      shouldDouble = !shouldDouble;
    }
    
    return (sum % 10) === 0;
  }
  
  // Función para validar fecha de expiración
  function validarFechaExpiracion(fecha) {
    if (!/^\d{2}\/\d{2}$/.test(fecha)) return false;
    
    const parts = fecha.split('/');
    const mes = parseInt(parts[0], 10);
    const año = parseInt('20' + parts[1], 10);
    
    if (mes < 1 || mes > 12) return false;
    
    const ahora = new Date();
    const currentYear = ahora.getFullYear();
    const currentMonth = ahora.getMonth() + 1;
    
    // La tarjeta no puede estar caducada
    if (año < currentYear || (año === currentYear && mes < currentMonth)) {
      return false;
    }
    
    return true;
  }
});

const modalPagos = document.getElementById('modalPagos');
if (modalPagos) {
  modalPagos.addEventListener('shown.bs.modal', () => {
    console.log('🟢 Modal abierto correctamente');
  });
  modalPagos.addEventListener('hidden.bs.modal', () => {
    console.log('🔴 Modal cerrado');
  });
}
