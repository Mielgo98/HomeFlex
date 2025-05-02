// Variables globales
let usuarioActual = null;
let contactoActual = null;
let propiedadActual = null;
let ultimoMensajeId = null;
let intervaloActualizacion = null;
let imagenSeleccionada = null;

// Obtiene el valor de una cookie
function getCookie(name) {
  const matches = document.cookie.match(
    new RegExp("(?:^|; )" +
      name.replace(/([.$?*|{}()[\]\\/+^])/g, '\\$1') +
      "=([^;]*)")
  );
  return matches ? decodeURIComponent(matches[1]) : null;
}

// Al cargar el documento
document.addEventListener('DOMContentLoaded', function() {
    // Obtener token JWT de la cookie
    const token = getCookie('jwt_token');
    
    // Intenta obtener el ID del usuario autenticado
    try {
        // Desde el elemento HTML
        const usuarioIdElement = document.getElementById('usuario-actual-id');
        if (usuarioIdElement && usuarioIdElement.dataset.id) {
            usuarioActual = usuarioIdElement.dataset.id;
            console.log("ID del usuario actual (desde HTML):", usuarioActual);
        }
    } catch (error) {
        console.error("Error al obtener ID de usuario:", error);
    }
    
    // Inicializar la interfaz
    inicializarInterfaz();
    
    // Agregar eventos a elementos
    agregarEventosElementos();
    
    // Establecer intervalo de actualización (cada 10 segundos)
    intervaloActualizacion = setInterval(function() {
        actualizarMensajes();
        actualizarContadorNoLeidos();
    }, 10000);
});

// Inicializar la interfaz
function inicializarInterfaz() {
    // Determinar la página actual
    const esListaMensajes = window.location.pathname.includes('/mensajes/lista');
    const esChat = window.location.pathname.includes('/mensajes/chat');
    
    if (esListaMensajes) {
        // Estamos en la lista de conversaciones
        cargarConversaciones();
    } else if (esChat) {
        // Estamos en la vista de chat
        const contactoIdElement = document.getElementById('contacto-id');
        const propiedadIdElement = document.getElementById('propiedad-id');
        
        if (contactoIdElement && contactoIdElement.value) {
            contactoActual = contactoIdElement.value;
            propiedadActual = propiedadIdElement && propiedadIdElement.value ? propiedadIdElement.value : null;
            
            cargarConversacion(contactoActual, propiedadActual);
        } else {
            // Intentar obtener contactoId de la URL
            const urlParams = new URLSearchParams(window.location.search);
            contactoActual = urlParams.get('contactoId');
            propiedadActual = urlParams.get('propiedadId');
            
            if (contactoActual) {
                cargarConversacion(contactoActual, propiedadActual);
            }
        }
    }
    
    // Actualizar contador de mensajes no leídos
    actualizarContadorNoLeidos();
}

// Agregar eventos a elementos
function agregarEventosElementos() {
    // Evento para enviar mensaje
    const formMensaje = document.getElementById('form-mensaje');
    if (formMensaje) {
        formMensaje.addEventListener('submit', function(e) {
            e.preventDefault();
            if (imagenSeleccionada) {
                enviarImagen();
            } else {
                enviarMensaje();
            }
        });
    }
    
    // Evento para la selección de imagen
    const imagenInput = document.getElementById('imagen-input');
    if (imagenInput) {
        imagenInput.addEventListener('change', function() {
            previewImagen(this);
        });
    }
    
    // Evento para cancelar la imagen seleccionada
    const cancelarImagen = document.getElementById('cancelar-imagen');
    if (cancelarImagen) {
        cancelarImagen.addEventListener('click', function() {
            resetearSeleccionImagen();
        });
    }
    
    // Evento para elementos de conversación (delegación)
    const conversacionesLista = document.querySelector('.conversaciones-lista');
    if (conversacionesLista) {
        conversacionesLista.addEventListener('click', function(e) {
            const conversacionItem = e.target.closest('.conversacion-item');
            if (conversacionItem) {
                const contactoId = conversacionItem.dataset.contactoId;
                const propiedadId = conversacionItem.dataset.propiedadId || '';
                window.location.href = `/mensajes/chat?contactoId=${contactoId}${propiedadId ? `&propiedadId=${propiedadId}` : ''}`;
            }
        });
    }
    
    // Evento para hacer clic en una imagen (ampliación)
    const chatMessages = document.querySelector('.chat-messages');
    if (chatMessages) {
        chatMessages.addEventListener('click', function(e) {
            const imagen = e.target.closest('.mensaje-imagen img');
            if (imagen) {
                mostrarImagenAmpliada(imagen.src);
            }
        });
    }
}

// Mostrar vista previa de imagen seleccionada
function previewImagen(input) {
    if (input.files && input.files[0]) {
        const reader = new FileReader();
        
        reader.onload = function(e) {
            const imagenPreview = document.getElementById('imagen-preview');
            const previewImg = imagenPreview.querySelector('img');
            
            previewImg.src = e.target.result;
            imagenPreview.classList.remove('d-none');
            
            // Guardar la imagen seleccionada
            imagenSeleccionada = input.files[0];
            
            // Deshabilitar el campo de texto
            document.getElementById('mensaje-input').disabled = true;
        }
        
        reader.readAsDataURL(input.files[0]);
    }
}

// Resetear selección de imagen
function resetearSeleccionImagen() {
    const imagenPreview = document.getElementById('imagen-preview');
    const imagenInput = document.getElementById('imagen-input');
    
    imagenPreview.classList.add('d-none');
    imagenInput.value = '';
    imagenSeleccionada = null;
    
    // Habilitar el campo de texto
    document.getElementById('mensaje-input').disabled = false;
}

// Mostrar imagen ampliada en modal
function mostrarImagenAmpliada(src) {
    // Crear modal solo si es necesario
    let modal = document.getElementById('image-modal');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'image-modal';
        modal.className = 'modal fade';
        modal.tabIndex = '-1';
        modal.role = 'dialog';
        modal.setAttribute('aria-hidden', 'true');
        
        modal.innerHTML = `
            <div class="modal-dialog modal-dialog-centered modal-lg">
                <div class="modal-content">
                    <div class="modal-body p-0">
                        <button type="button" class="close position-absolute" style="top: 10px; right: 15px; color: white; z-index: 1050; text-shadow: 0 0 3px rgba(0,0,0,0.5);" data-dismiss="modal" aria-label="Close">
                            <span aria-hidden="true">&times;</span>
                        </button>
                        <img src="" class="img-fluid" alt="Imagen ampliada">
                    </div>
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
    }
    
    // Establecer la imagen
    const modalImg = modal.querySelector('img');
    modalImg.src = src;
    
    // Mostrar modal
    $(modal).modal('show');
}

// Cargar lista de conversaciones
function cargarConversaciones() {
    const token = getCookie('jwt_token');
    
    fetch('/api/mensajes', {
        headers: {
            'Authorization': 'Bearer ' + token,
            'Accept': 'application/json'
        }
    })
    .then(response => {
        if (response.status === 401) {
            window.location = '/login';
            throw new Error('No autorizado');
        }
        return response.json();
    })
    .then(data => {
        mostrarConversaciones(data);
    })
    .catch(error => {
        console.error('Error:', error);
        mostrarNotificacion('Error al cargar las conversaciones', 'error');
    });
}

// Mostrar lista de conversaciones
function mostrarConversaciones(conversaciones) {
    const conversacionesLista = document.querySelector('.conversaciones-lista');
    if (!conversacionesLista) return;
    
    conversacionesLista.innerHTML = '';
    
    if (conversaciones.length === 0) {
        conversacionesLista.innerHTML = `
            <div class="no-conversacion">
                <i class="fas fa-comments"></i>
                <h3>Sin conversaciones</h3>
                <p>No tienes conversaciones activas. Cuando inicies una conversación con otro usuario, aparecerá aquí.</p>
            </div>
        `;
        return;
    }
    
    conversaciones.forEach(conv => {
        const fechaFormateada = formatearFecha(conv.fechaUltimoMensaje);
        
        const elemento = document.createElement('div');
        elemento.className = 'conversacion-item';
        elemento.dataset.contactoId = conv.contactoId;
        if (conv.propiedadId) {
            elemento.dataset.propiedadId = conv.propiedadId;
        }
        
        let mensajeTexto = conv.ultimoMensaje;
        if (conv.tipoUltimoMensaje === 'imagen') {
            mensajeTexto = '📷 Imagen';
        }
        
        elemento.innerHTML = `
            <img src="${conv.contactoFoto || '/img/avatar-default.png'}" alt="${conv.contactoNombre}" class="conversacion-avatar">
            <div class="conversacion-info">
                <div class="conversacion-header">
                    <div class="conversacion-nombre">${conv.contactoNombre} ${conv.contactoApellidos}</div>
                    <div class="conversacion-fecha">${fechaFormateada}</div>
                </div>
                <div class="d-flex align-items-center">
                    <div class="conversacion-mensaje">
                        ${conv.propiedadTitulo ? `<small class="text-muted">[${conv.propiedadTitulo}]</small> ` : ''}
                        ${mensajeTexto}
                    </div>
                    ${conv.mensajesNoLeidos > 0 ? `<div class="conversacion-badge">${conv.mensajesNoLeidos}</div>` : ''}
                </div>
            </div>
        `;
        
        conversacionesLista.appendChild(elemento);
    });
}

// Cargar conversación específica
function cargarConversacion(contactoId, propiedadId) {
    const token = getCookie('jwt_token');
    
    let url = `/api/mensajes/${contactoId}`;
    if (propiedadId) {
        url += `?propiedadId=${propiedadId}`;
    }
    
    fetch(url, {
        headers: {
            'Authorization': 'Bearer ' + token,
            'Accept': 'application/json'
        }
    })
    .then(response => {
        if (response.status === 401) {
            window.location = '/login';
            throw new Error('No autorizado');
        }
        return response.json();
    })
    .then(data => {
        mostrarMensajes(data);
        if (data.length > 0) {
            ultimoMensajeId = data[data.length - 1].id;
        }
    })
    .catch(error => {
        console.error('Error:', error);
        mostrarNotificacion('Error al cargar los mensajes', 'error');
    });
}
// Función para mostrar los mensajes con estilo WhatsApp - versión final
function mostrarMensajes(mensajes) {
    const chatMessages = document.querySelector('.chat-messages');
    if (!chatMessages) return;
    
    chatMessages.innerHTML = '';
    
    if (mensajes.length === 0) {
        chatMessages.innerHTML = `
            <div class="no-conversacion">
                <i class="far fa-comment-dots"></i>
                <h3>No hay mensajes</h3>
                <p>Envía el primer mensaje para iniciar la conversación.</p>
            </div>
        `;
        return;
    }

    // Mostrar el banner de cifrado
    const bannerCifrado = document.createElement('div');
    bannerCifrado.className = 'mensaje-cifrado';
    bannerCifrado.innerHTML = `
        <div class="cifrado-contenido">
            <i class="fas fa-lock"></i>
            Los mensajes y las llamadas están cifrados de extremo a extremo. Nadie fuera de este chat, ni
            siquiera HomeFlex, puede leerlos ni escucharlos.
            Toca para obtener más información.
        </div>
    `;
    chatMessages.appendChild(bannerCifrado);
    
    // Crear un separador de fecha único para 'HOY'
    const separador = document.createElement('div');
    separador.className = 'fecha-separador';
    separador.innerHTML = '<span>HOY</span>';
    chatMessages.appendChild(separador);
    
    // Añadir todos los mensajes con la fecha de la DB directamente
    mensajes.forEach(mensaje => {
        // Determinar si el mensaje es propio comparando con el contacto actual
        const esPropio = contactoActual && (mensaje.receptorId == contactoActual);
        
        console.log(`Mensaje ID: ${mensaje.id}, Emisor: ${mensaje.emisorId}, Receptor: ${mensaje.receptorId}, Contacto: ${contactoActual}, ¿Es propio?: ${esPropio}`);
        
        // Formatear la hora directamente desde la BD
        let horaFormateada;
        try {
            // Obtenemos la fecha de la base de datos
            // Extrae directamente de la fecha original en formato 2025-05-02 12:32:59.963
            if (mensaje.fechaEnvio) {
                // Verificar el formato de fechaEnvio
                console.log(`Tipo de fechaEnvio para mensaje ID ${mensaje.id}:`, typeof mensaje.fechaEnvio);
                
                if (Array.isArray(mensaje.fechaEnvio)) {
                    // Si es un array [año, mes, día, hora, minuto, segundo, ms]
                    // Usar las posiciones 3 y 4 que corresponden a hora y minuto
                    let hora = parseInt(mensaje.fechaEnvio[3]);
                    const minuto = String(mensaje.fechaEnvio[4]).padStart(2, '0');
                    
                    // Convertir a formato 12h con AM/PM
                    const ampm = hora >= 12 ? 'p.m.' : 'a.m.';
                    hora = hora % 12;
                    hora = hora ? hora : 12; // 0 -> 12
                    
                    horaFormateada = `${hora}:${minuto} ${ampm}`;
                } else if (typeof mensaje.fechaEnvio === 'string') {
                    // Si es un string como "2025-05-02 12:32:59.963"
                    const partes = mensaje.fechaEnvio.split(' ');
                    if (partes.length === 2) {
                        const horaParts = partes[1].split(':');
                        if (horaParts.length >= 2) {
                            let hora = parseInt(horaParts[0]);
                            const minuto = horaParts[1];
                            
                            // Convertir a formato 12h con AM/PM
                            const ampm = hora >= 12 ? 'p.m.' : 'a.m.';
                            hora = hora % 12;
                            hora = hora ? hora : 12; // 0 -> 12
                            
                            horaFormateada = `${hora}:${minuto} ${ampm}`;
                        } else {
                            horaFormateada = "12:00 p.m.";
                        }
                    } else {
                        horaFormateada = "12:00 p.m.";
                    }
                } else {
                    // Si es un objeto Date o cualquier otro formato
                    const fecha = new Date(mensaje.fechaEnvio);
                    if (!isNaN(fecha.getTime())) {
                        let horas = fecha.getHours();
                        const minutos = fecha.getMinutes().toString().padStart(2, '0');
                        const ampm = horas >= 12 ? 'p.m.' : 'a.m.';
                        horas = horas % 12;
                        horas = horas ? horas : 12; // 0 -> 12
                        
                        horaFormateada = `${horas}:${minutos} ${ampm}`;
                    } else {
                        horaFormateada = "12:00 p.m.";
                    }
                }
            } else {
                horaFormateada = "12:00 p.m.";
            }
        } catch (error) {
            console.error(`Error al formatear hora para mensaje ID ${mensaje.id}:`, error);
            horaFormateada = "12:00 p.m.";
        }
        
        console.log(`Mensaje ID: ${mensaje.id}, Hora original:`, mensaje.fechaEnvio, `Hora formateada: ${horaFormateada}`);
        
        const elemento = document.createElement('div');
        elemento.className = `mensaje ${esPropio ? 'mensaje-enviado' : 'mensaje-recibido'}`;
        elemento.dataset.id = mensaje.id || '';
        elemento.dataset.emisor = mensaje.emisorId; // Guardar el emisor para referencia
        
        // Mostrar el mensaje según su tipo
        if (mensaje.tipoMensaje === 'imagen') {
            elemento.classList.add('mensaje-imagen');
            elemento.innerHTML = `
                <img src="${mensaje.urlRecurso}" alt="Imagen compartida">
                <div class="mensaje-fecha">${horaFormateada}</div>
            `;
        } else {
            // Mensaje de texto por defecto
            elemento.innerHTML = `
                <div class="mensaje-contenido">${mensaje.contenido}</div>
                <div class="mensaje-fecha">${horaFormateada}</div>
            `;
        }
        
        chatMessages.appendChild(elemento);
    });
    
    // Desplazarse al último mensaje
    chatMessages.scrollTop = chatMessages.scrollHeight;
}

// Función auxiliar para formatear las fechas de los grupos
function formatearFechaGrupo(fecha) {
    // Verificar que la fecha es válida
    if (!fecha || isNaN(fecha.getTime())) {
        console.warn("Fecha inválida en formatearFechaGrupo:", fecha);
        return "HOY"; // Usar HOY como fallback
    }
    
    try {
        const ahora = new Date();
        const ayer = new Date();
        ayer.setDate(ahora.getDate() - 1);
        
        if (fecha.toDateString() === ahora.toDateString()) {
            return 'HOY';
        }
        
        if (fecha.toDateString() === ayer.toDateString()) {
            return 'AYER';
        }
        
        const dia = fecha.getDate();
        const meses = ['ENERO', 'FEBRERO', 'MARZO', 'ABRIL', 'MAYO', 'JUNIO', 'JULIO', 'AGOSTO', 'SEPTIEMBRE', 'OCTUBRE', 'NOVIEMBRE', 'DICIEMBRE'];
        const mes = meses[fecha.getMonth()];
        const anio = fecha.getFullYear();
        
        return `${dia} DE ${mes} DE ${anio}`;
    } catch (error) {
        console.error("Error en formatearFechaGrupo:", error);
        return "HOY"; // Usar HOY como fallback
    }
}

// Función mejorada para formatear fechas al estilo WhatsApp
function formatearFecha(fechaInput) {
    // Verificar si la fecha es válida
    if (!fechaInput) {
        // Obtener fecha actual si no hay fecha
        const ahora = new Date();
        return formatearFechaObj(ahora);
    }
    
    // Si ya es un objeto Date, usarlo directamente
    let fecha;
    if (fechaInput instanceof Date) {
        fecha = fechaInput;
    } else {
        try {
            fecha = new Date(fechaInput);
            
            // Verificar si la fecha resultante es válida
            if (isNaN(fecha.getTime())) {
                console.log("Fecha inválida después de conversión:", fechaInput);
                // Usar fecha actual como fallback
                const ahora = new Date();
                return formatearFechaObj(ahora);
            }
        } catch (error) {
            console.error("Error al convertir fecha:", error);
            // Usar fecha actual como fallback
            const ahora = new Date();
            return formatearFechaObj(ahora);
        }
    }
    
    return formatearFechaObj(fecha);
}

// Función auxiliar para formatear un objeto Date
function formatearFechaObj(fecha) {
    try {
        const ahora = new Date();
        const ayer = new Date();
        ayer.setDate(ahora.getDate() - 1);
        
        // Formatear hora en formato 12h con AM/PM
        let horas = fecha.getHours();
        const minutos = fecha.getMinutes().toString().padStart(2, '0');
        // Opcional: también puedes añadir los segundos si lo deseas
        // const segundos = fecha.getSeconds().toString().padStart(2, '0');
        const ampm = horas >= 12 ? 'p.m.' : 'a.m.';
        horas = horas % 12;
        horas = horas ? horas : 12; // la hora '0' debe ser '12'
        
        // Usar este formato para incluir minutos exactos
        const horaFormateada = `${horas}:${minutos} ${ampm}`;
        
        // Si quieres incluir segundos, usa esta línea en su lugar
        // const horaFormateada = `${horas}:${minutos}:${segundos} ${ampm}`;
        
        // Si es hoy
        if (fecha.toDateString() === ahora.toDateString()) {
            return horaFormateada;
        }
        
        // Si es ayer
        if (fecha.toDateString() === ayer.toDateString()) {
            return `Ayer, ${horaFormateada}`;
        }
        
        // Formato para otras fechas
        const dia = fecha.getDate();
        const mes = fecha.getMonth() + 1;
        const anio = fecha.getFullYear();
        
        return `${dia}/${mes}/${anio}, ${horaFormateada}`;
    } catch (error) {
        console.error("Error al formatear fecha:", error);
        return "Ahora"; // fallback
    }
}
// Enviar un nuevo mensaje de texto
function enviarMensaje() {
    const token = getCookie('jwt_token');
    const inputMensaje = document.getElementById('mensaje-input');
    if (!inputMensaje || !inputMensaje.value.trim()) return;
    
    const contenido = inputMensaje.value.trim();
    
    // Datos del mensaje
    const datos = {
        receptorId: contactoActual,
        contenido: contenido,
        tipoMensaje: 'texto'
    };
    
    // Añadir propiedadId si existe
    if (propiedadActual) {
        datos.propiedadId = propiedadActual;
    }
    
    // Enviar mediante API
    fetch('/api/mensajes', {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + token,
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        },
        body: JSON.stringify(datos)
    })
    .then(response => {
        if (response.status === 401) {
            window.location = '/login';
            throw new Error('No autorizado');
        }
        if (!response.ok) {
            throw new Error('Error al enviar mensaje');
        }
        return response.json();
    })
    .then(data => {
        // Limpiar input
        inputMensaje.value = '';
        
        // Verificar si hay que crear un nuevo separador de fecha
        const fecha = new Date(data.fechaEnvio || new Date());
        const fechaFormatoGrupo = formatearFechaGrupo(fecha);
        
        // Verificar si necesitamos un nuevo separador de fecha
        let necesitaSeparador = true;
        const chatMessages = document.querySelector('.chat-messages');
        const separadores = chatMessages.querySelectorAll('.fecha-separador span');
        
        for (let i = 0; i < separadores.length; i++) {
            if (separadores[i].textContent === fechaFormatoGrupo) {
                necesitaSeparador = false;
                break;
            }
        }
        
        // Agregar separador si es necesario
        if (necesitaSeparador) {
            const separador = document.createElement('div');
            separador.className = 'fecha-separador';
            separador.innerHTML = `<span>${fechaFormatoGrupo}</span>`;
            chatMessages.appendChild(separador);
        }
        
        // Añadir mensaje a la conversación
        const elemento = document.createElement('div');
        elemento.className = 'mensaje mensaje-enviado';
        elemento.dataset.id = data.id || '';
        elemento.dataset.emisor = usuarioActual; // Asegurarnos de guardar el emisor
        
        elemento.innerHTML = `
            <div class="mensaje-contenido">${data.contenido}</div>
            <div class="mensaje-fecha">${formatearFecha(data.fechaEnvio)}</div>
        `;
        
        chatMessages.appendChild(elemento);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        
        // Actualizar último ID
        ultimoMensajeId = data.id;
    })
    .catch(error => {
        console.error('Error:', error);
        mostrarNotificacion('Error al enviar el mensaje', 'error');
    });
}

// Enviar una imagen
function enviarImagen() {
    const token = getCookie('jwt_token');
    if (!imagenSeleccionada) return;
    
    const formData = new FormData();
    formData.append('imagen', imagenSeleccionada);
    formData.append('receptorId', contactoActual);
    
    if (propiedadActual) {
        formData.append('propiedadId', propiedadActual);
    }
    
    // Mostrar indicador de carga
    const chatMessages = document.querySelector('.chat-messages');
    const loadingElement = document.createElement('div');
    loadingElement.className = 'mensaje mensaje-enviado mensaje-cargando';
    loadingElement.innerHTML = `
        <div class="spinner-border spinner-border-sm text-primary" role="status">
            <span class="sr-only">Enviando imagen...</span>
        </div>
        <span class="ml-2">Enviando imagen...</span>
    `;
    chatMessages.appendChild(loadingElement);
    chatMessages.scrollTop = chatMessages.scrollHeight;
    
    // Enviar imagen
    fetch('/api/mensajes/imagen', {
        method: 'POST',
        headers: {
            'Authorization': 'Bearer ' + token,
            'Accept': 'application/json'
        },
        body: formData
    })
    .then(response => {
        if (response.status === 401) {
            window.location = '/login';
            throw new Error('No autorizado');
        }
        if (!response.ok) {
            throw new Error('Error al enviar imagen');
        }
        return response.json();
    })
    .then(data => {
        // Eliminar indicador de carga
        const mensajeCargando = document.querySelector('.mensaje-cargando');
        if (mensajeCargando) {
            mensajeCargando.remove();
        }
        
        // Resetear selección de imagen
        resetearSeleccionImagen();
        
        // Verificar si hay que crear un nuevo separador de fecha
        const fecha = new Date();
        const fechaFormatoGrupo = formatearFechaGrupo(fecha);
        
        // Verificar si necesitamos un nuevo separador de fecha
        let necesitaSeparador = true;
        const separadores = chatMessages.querySelectorAll('.fecha-separador span');
        
        for (let i = 0; i < separadores.length; i++) {
            if (separadores[i].textContent === fechaFormatoGrupo) {
                necesitaSeparador = false;
                break;
            }
        }
        
        // Agregar separador si es necesario
        if (necesitaSeparador) {
            const separador = document.createElement('div');
            separador.className = 'fecha-separador';
            separador.innerHTML = `<span>${fechaFormatoGrupo}</span>`;
            chatMessages.appendChild(separador);
        }
        
        // Añadir mensaje con imagen a la conversación
        const elemento = document.createElement('div');
        elemento.className = 'mensaje mensaje-enviado mensaje-imagen';
        elemento.dataset.id = data.mensajeId || '';
        elemento.dataset.emisor = usuarioActual; // Asegurarnos de guardar el emisor
        
        elemento.innerHTML = `
            <img src="${data.url}" alt="Imagen compartida">
            <div class="mensaje-fecha">${formatearFecha(new Date())}</div>
        `;
        
        chatMessages.appendChild(elemento);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        
        // Actualizar último ID
        ultimoMensajeId = data.mensajeId;
    })
    .catch(error => {
        console.error('Error:', error);
        
        // Eliminar indicador de carga
        const mensajeCargando = document.querySelector('.mensaje-cargando');
        if (mensajeCargando) {
            mensajeCargando.remove();
        }
        
        mostrarNotificacion('Error al enviar la imagen', 'error');
    });
}

// Actualizar mensajes (para recibir nuevos)
function actualizarMensajes() {
    // Solo si estamos en una conversación
    if (!contactoActual) return;
    
    const token = getCookie('jwt_token');
    
    let url = `/api/mensajes/${contactoActual}`;
    if (propiedadActual) {
        url += `?propiedadId=${propiedadActual}`;
    }
    
    fetch(url, {
        headers: {
            'Authorization': 'Bearer ' + token,
            'Accept': 'application/json'
        }
    })
    .then(response => {
        if (response.status === 401) {
            window.location = '/login';
            throw new Error('No autorizado');
        }
        if (!response.ok) {
            throw new Error('Error al actualizar mensajes');
        }
        return response.json();
    })
    .then(data => {
        // Solo añadir mensajes nuevos
        if (data.length > 0 && ultimoMensajeId) {
            const nuevosMensajes = data.filter(m => m.id > ultimoMensajeId);
            
            if (nuevosMensajes.length > 0) {
                const chatMessages = document.querySelector('.chat-messages');
                
                nuevosMensajes.forEach(mensaje => {
                    // Convertimos a string para comparar correctamente
                    const emisorId = String(mensaje.emisorId || '');
                    const usuarioId = String(usuarioActual || '');
                    
                    // Verificar si el mensaje es propio
                    const esPropio = emisorId === usuarioId;
                    
                    console.log(`Mensaje nuevo - ID: ${mensaje.id}, Emisor: ${emisorId}, Usuario actual: ${usuarioId}, ¿Es propio?: ${esPropio}`);
                    
                    const fechaFormateada = formatearFecha(mensaje.fechaEnvio);
                    
                    // Verificar si ya existe el separador de fecha para esta fecha
                    const fecha = new Date(mensaje.fechaEnvio || new Date());
                    const fechaFormatoGrupo = formatearFechaGrupo(fecha);
                    
                    // Verificar si necesitamos un nuevo separador de fecha
                    let necesitaSeparador = true;
                    const separadores = chatMessages.querySelectorAll('.fecha-separador span');
                    
                    for (let i = 0; i < separadores.length; i++) {
                        if (separadores[i].textContent === fechaFormatoGrupo) {
                            necesitaSeparador = false;
                            break;
                        }
                    }
                    
                    // Agregar separador si es necesario
                    if (necesitaSeparador) {
                        const separador = document.createElement('div');
                        separador.className = 'fecha-separador';
                        separador.innerHTML = `<span>${fechaFormatoGrupo}</span>`;
                        chatMessages.appendChild(separador);
                    }
                    
					// Crear el elemento de mensaje
					                    const elemento = document.createElement('div');
					                    elemento.className = `mensaje ${esPropio ? 'mensaje-enviado' : 'mensaje-recibido'}`;
					                    elemento.dataset.id = mensaje.id || '';
					                    elemento.dataset.emisor = emisorId; // Guardar el emisor para referencia
					                    
					                    // Mostrar el mensaje según su tipo
					                    if (mensaje.tipoMensaje === 'imagen') {
					                        elemento.classList.add('mensaje-imagen');
					                        elemento.innerHTML = `
					                            <img src="${mensaje.urlRecurso}" alt="Imagen compartida">
					                            <div class="mensaje-fecha">${fechaFormateada}</div>
					                        `;
					                    } else {
					                        elemento.innerHTML = `
					                            <div class="mensaje-contenido">${mensaje.contenido}</div>
					                            <div class="mensaje-fecha">${fechaFormateada}</div>
					                        `;
					                    }
					                    
					                    chatMessages.appendChild(elemento);
					                });
					                
					                chatMessages.scrollTop = chatMessages.scrollHeight;
					                ultimoMensajeId = data[data.length - 1].id;
					            }
					        }
					    })
					    .catch(error => {
					        console.error('Error:', error);
					    });
					}

					// Actualizar contador de mensajes no leídos
					function actualizarContadorNoLeidos() {
					    const token = getCookie('jwt_token');
					    
					    fetch('/api/mensajes/no-leidos', {
					        headers: {
					            'Authorization': 'Bearer ' + token,
					            'Accept': 'application/json'
					        }
					    })
					    .then(response => {
					        if (response.status === 401) {
					            // No redirigimos aquí para evitar interrupciones
					            throw new Error('No autorizado');
					        }
					        if (!response.ok) {
					            throw new Error('Error al obtener mensajes no leídos');
					        }
					        return response.json();
					    })
					    .then(data => {
					        const contadorElement = document.getElementById('contador-mensajes');
					        if (contadorElement) {
					            const total = data.total;
					            
					            if (total > 0) {
					                contadorElement.textContent = total;
					                contadorElement.classList.remove('d-none');
					            } else {
					                contadorElement.classList.add('d-none');
					            }
					        }
					    })
					    .catch(error => {
					        console.error('Error:', error);
					    });
					}

					// Mostrar notificación
					function mostrarNotificacion(mensaje, tipo = 'success') {
					    // Si existe SweetAlert2
					    if (typeof Swal !== 'undefined') {
					        Swal.fire({
					            title: tipo === 'error' ? 'Error' : 'Éxito',
					            text: mensaje,
					            icon: tipo,
					            toast: true,
					            position: 'top-end',
					            showConfirmButton: false,
					            timer: 3000
					        });
					    } else if (typeof toastr !== 'undefined') {
					        // Si existe Toastr
					        toastr[tipo](mensaje);
					    } else {
					        // Fallback a alert
					        alert(mensaje);
					    }
					}

					// Modal para ampliación de imágenes
					function agregarModalImagenes() {
					    // Verificar si ya existe
					    if (document.getElementById('image-modal')) return;
					    
					    // Crear modal para visualizar imágenes ampliadas
					    const modal = document.createElement('div');
					    modal.id = 'image-modal';
					    modal.className = 'modal fade';
					    modal.tabIndex = '-1';
					    modal.setAttribute('aria-hidden', 'true');
					    
					    modal.innerHTML = `
					        <div class="modal-dialog modal-lg modal-dialog-centered">
					            <div class="modal-content bg-transparent border-0">
					                <div class="modal-body p-0 text-center">
					                    <button type="button" class="close position-absolute" style="top: 15px; right: 15px; color: white; font-size: 30px; text-shadow: 0 0 5px rgba(0,0,0,0.5);" data-dismiss="modal" aria-label="Close">
					                        <span aria-hidden="true">&times;</span>
					                    </button>
					                    <img src="" class="img-fluid rounded" style="max-height: 90vh;" alt="Imagen ampliada">
					                </div>
					            </div>
					        </div>
					    `;
					    
					    document.body.appendChild(modal);
					}

					// Al cargar la página, añadir el modal para imágenes
					document.addEventListener('DOMContentLoaded', function() {
					    agregarModalImagenes();
					});