document.addEventListener('DOMContentLoaded', function() {
    // Referencias a elementos DOM
    const form = document.getElementById('formPublicarPropiedad');
    const steps = document.querySelectorAll('.step');
    const nextButtons = document.querySelectorAll('.next-step');
    const prevButtons = document.querySelectorAll('.prev-step');
    const fotosInput = document.getElementById('fotos');
    const previewContainer = document.getElementById('preview-container');
    const fotoPrincipalSelect = document.getElementById('fotoPrincipal');
    const btnAyuda = document.getElementById('btnAyuda');
    
    let map, marker;
    let currentStep = 0;
    let selectedFiles = [];
    
    // Inicializar el mapa en el paso 3
    function initMap() {
        // Coordenadas por defecto (Madrid, España)
        const defaultLat = 40.416775;
        const defaultLng = -3.703790;
        
        // Crear mapa
        map = L.map('map').setView([defaultLat, defaultLng], 13);
        
        // Añadir capa de OpenStreetMap
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);
        
        // Añadir marcador arrastrable
        marker = L.marker([defaultLat, defaultLng], {
            draggable: true
        }).addTo(map);
        
        // Actualizar campos de latitud y longitud al arrastrar el marcador
        marker.on('dragend', function(event) {
            const position = marker.getLatLng();
            document.getElementById('latitud').value = position.lat.toFixed(8);
            document.getElementById('longitud').value = position.lng.toFixed(8);
        });
        
        // Establecer valores iniciales
        document.getElementById('latitud').value = defaultLat.toFixed(8);
        document.getElementById('longitud').value = defaultLng.toFixed(8);
        
        // Buscar ubicación cuando se completan los campos de dirección, ciudad y país
        document.getElementById('step2').addEventListener('transitionend', function() {
            if (currentStep === 2) {
                const direccion = document.getElementById('direccion').value;
                const ciudad = document.getElementById('ciudad').value;
                const pais = document.getElementById('pais').value;
                
                if (direccion && ciudad && pais) {
                    const searchQuery = `${direccion}, ${ciudad}, ${pais}`;
                    geocodeAddress(searchQuery);
                }
            }
        });
    }
    
    // Función para geocodificar una dirección
    function geocodeAddress(address) {
        // Utilizando Nominatim de OpenStreetMap para geocodificación
        fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(address)}`)
            .then(response => response.json())
            .then(data => {
                if (data && data.length > 0) {
                    const lat = parseFloat(data[0].lat);
                    const lon = parseFloat(data[0].lon);
                    
                    // Actualizar mapa y marcador
                    map.setView([lat, lon], 16);
                    marker.setLatLng([lat, lon]);
                    
                    // Actualizar campos
                    document.getElementById('latitud').value = lat.toFixed(8);
                    document.getElementById('longitud').value = lon.toFixed(8);
                    
                    // Mostrar notificación
                    Swal.fire({
                        title: '¡Ubicación encontrada!',
                        text: 'Hemos localizado tu dirección en el mapa. Puedes ajustar el marcador si es necesario.',
                        icon: 'success',
                        toast: true,
                        position: 'top-end',
                        showConfirmButton: false,
                        timer: 3000
                    });
                }
            })
            .catch(error => {
                console.error('Error en geocodificación:', error);
                Swal.fire({
                    title: 'Error de ubicación',
                    text: 'No pudimos encontrar la dirección exacta. Por favor, ajusta el marcador manualmente.',
                    icon: 'warning',
                    confirmButtonText: 'Entendido'
                });
            });
    }
    
    // Mostrar el paso actual
    function showStep(stepIndex) {
        steps.forEach((step, index) => {
            if (index === stepIndex) {
                step.classList.remove('d-none');
                // Añadir clase para animación de entrada
                step.classList.add('step-enter');
                setTimeout(() => {
                    step.classList.remove('step-enter');
                }, 10);
            } else {
                step.classList.add('d-none');
            }
        });
        
        // Si es el paso del mapa, inicializarlo
        if (stepIndex === 2 && !map) {
            setTimeout(initMap, 100);
        }
        
        currentStep = stepIndex;
    }
    
    // Validar el paso actual
    function validateCurrentStep() {
        const currentStepEl = steps[currentStep];
        const inputs = currentStepEl.querySelectorAll('input[required], select[required], textarea[required]');
        let isValid = true;
        
        inputs.forEach(input => {
            if (!input.value.trim()) {
                input.classList.add('is-invalid');
                isValid = false;
            } else {
                input.classList.remove('is-invalid');
            }
            
            // Validaciones específicas
            if (input.id === 'capacidad' && parseInt(input.value) < 1) {
                input.classList.add('is-invalid');
                isValid = false;
            }
            
            if (input.id === 'precioDia' && parseFloat(input.value) <= 0) {
                input.classList.add('is-invalid');
                isValid = false;
            }
        });
        
        return isValid;
    }
    
    // Manejo de cambio de paso
    nextButtons.forEach(button => {
        button.addEventListener('click', () => {
            if (validateCurrentStep()) {
                showStep(currentStep + 1);
            } else {
                Swal.fire({
                    title: 'Datos incompletos',
                    text: 'Por favor, completa todos los campos requeridos antes de continuar.',
                    icon: 'error',
                    confirmButtonText: 'Entendido'
                });
            }
        });
    });
    
    prevButtons.forEach(button => {
        button.addEventListener('click', () => {
            showStep(currentStep - 1);
        });
    });
    
    // Gestión de previsualización de imágenes
    fotosInput.addEventListener('change', function(e) {
        previewContainer.innerHTML = '';
        fotoPrincipalSelect.innerHTML = '';
        selectedFiles = Array.from(e.target.files);
        
        if (selectedFiles.length === 0) {
            fotosInput.classList.add('is-invalid');
            return;
        } else {
            fotosInput.classList.remove('is-invalid');
        }
        
        selectedFiles.forEach((file, index) => {
            // Crear elemento de previsualización
            const previewCol = document.createElement('div');
            previewCol.className = 'col-6 col-md-4 col-lg-3 preview-item';
            
            // Crear imagen
            const img = document.createElement('img');
            img.className = 'preview-img';
            img.src = URL.createObjectURL(file);
            img.alt = `Foto ${index + 1}`;
            
            // Botón para eliminar
            const deleteBtn = document.createElement('div');
            deleteBtn.className = 'preview-delete';
            deleteBtn.innerHTML = '<i class="bi bi-x"></i>';
            deleteBtn.onclick = function() {
                previewCol.remove();
                updateFileSelection(index);
            };
            
            // Si es la primera imagen, marcarla como principal
            if (index === 0) {
                const primaryBadge = document.createElement('div');
                primaryBadge.className = 'preview-primary';
                primaryBadge.textContent = 'Principal';
                previewCol.appendChild(primaryBadge);
            }
            
            // Añadir elementos
            previewCol.appendChild(img);
            previewCol.appendChild(deleteBtn);
            previewContainer.appendChild(previewCol);
            
            // Añadir opción al selector de foto principal
            const option = document.createElement('option');
            option.value = index;
            option.textContent = `Foto ${index + 1}`;
            if (index === 0) option.selected = true;
            fotoPrincipalSelect.appendChild(option);
        });
    });
    
    // Actualizar selección de archivos al eliminar una previsualización
    function updateFileSelection(removedIndex) {
        const newFiles = new DataTransfer();
        selectedFiles = selectedFiles.filter((_, index) => index !== removedIndex);
        
        selectedFiles.forEach(file => {
            newFiles.items.add(file);
        });
        
        fotosInput.files = newFiles.files;
        
        // Actualizar selector de foto principal
        fotoPrincipalSelect.innerHTML = '';
        selectedFiles.forEach((_, index) => {
            const option = document.createElement('option');
            option.value = index;
            option.textContent = `Foto ${index + 1}`;
            fotoPrincipalSelect.appendChild(option);
        });
    }
    
    // Evento de cambio de foto principal
    fotoPrincipalSelect.addEventListener('change', function() {
        const selectedIndex = parseInt(this.value);
        const primaryBadges = document.querySelectorAll('.preview-primary');
        
        // Eliminar todas las etiquetas de principal
        primaryBadges.forEach(badge => badge.remove());
        
        // Añadir etiqueta al seleccionado
        const previewItems = document.querySelectorAll('.preview-item');
        if (previewItems[selectedIndex]) {
            const primaryBadge = document.createElement('div');
            primaryBadge.className = 'preview-primary';
            primaryBadge.textContent = 'Principal';
            previewItems[selectedIndex].appendChild(primaryBadge);
        }
    });
    
    // Manejo del envío del formulario
    form.addEventListener('submit', function(e) {
        e.preventDefault();
        
        // Validar último paso
        if (!validateCurrentStep()) {
            Swal.fire({
                title: 'Datos incompletos',
                text: 'Por favor, completa todos los campos requeridos antes de enviar.',
                icon: 'error',
                confirmButtonText: 'Entendido'
            });
            return;
        }
        
        // Mostrar indicador de carga
        Swal.fire({
            title: 'Publicando propiedad',
            html: 'Estamos procesando tu solicitud...',
            allowOutsideClick: false,
            didOpen: () => {
                Swal.showLoading();
            }
        });
        
        // Crear FormData con todos los datos
        const formData = new FormData(form);
        
        // Enviar formulario mediante fetch
        fetch(form.action, {
            method: 'POST',
            body: formData,
            credentials: 'same-origin'
        })
        .then(response => {
            if (!response.ok) {
                throw new Error('Error al publicar la propiedad');
            }
            return response.json();
        })
        .then(data => {
            // Comprobar si el usuario necesita actualizar el rol
            const rolActualizado = data.rolActualizado || false;
            
            Swal.fire({
                title: '¡Propiedad publicada!',
                html: rolActualizado 
                    ? '¡Enhorabuena! Tu propiedad ha sido publicada correctamente.<br><br><strong>Tu perfil ha sido actualizado a Propietario.</strong>'
                    : 'Tu propiedad ha sido publicada correctamente.',
                icon: 'success',
                confirmButtonText: 'Ver mis propiedades'
            }).then((result) => {
                if (result.isConfirmed) {
                    window.location.href = '/propiedades/mis-propiedades';
                }
            });
        })
        .catch(error => {
            console.error('Error:', error);
            Swal.fire({
                title: 'Error',
                text: 'Ha ocurrido un error al publicar la propiedad. Por favor, inténtalo de nuevo.',
                icon: 'error',
                confirmButtonText: 'Entendido'
            });
        });
    });
    
    // Botón de ayuda
    btnAyuda.addEventListener('click', function() {
        Swal.fire({
            title: 'Consejos para publicar',
            html: `
                <div class="text-start">
                    <p><i class="bi bi-check-circle-fill text-success me-2"></i> Usa un <strong>título atractivo</strong> que destaque lo mejor de tu propiedad.</p>
                    <p><i class="bi bi-check-circle-fill text-success me-2"></i> Incluye <strong>fotos de calidad</strong> y bien iluminadas de todas las estancias.</p>
                    <p><i class="bi bi-check-circle-fill text-success me-2"></i> Escribe una <strong>descripción detallada</strong> mencionando comodidades y servicios cercanos.</p>
                    <p><i class="bi bi-check-circle-fill text-success me-2"></i> <strong>Selecciona bien la ubicación</strong> en el mapa para facilitar la llegada de los huéspedes.</p>
                    <p><i class="bi bi-check-circle-fill text-success me-2"></i> Si ofreces <strong>descuentos por semana</strong>, asegúrate de configurar el precio semanal.</p>
                </div>
            `,
            icon: 'info',
            confirmButtonText: 'Entendido'
        });
    });
    
    // Mostrar el primer paso al cargar
    showStep(0);
});