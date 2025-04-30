// static/js/detalle-reserva.js
document.addEventListener('DOMContentLoaded', function() {
    // Referencias a elementos del DOM
    const cancelForm = document.querySelector('form[action*="/cancelar"]');
    const rejectForm = document.querySelector('form[action*="/rechazar"]');
    
    // Manejar el envío de formulario de cancelación para confirmar
    if (cancelForm) {
        cancelForm.addEventListener('submit', function(event) {
            const motivoTextarea = this.querySelector('textarea[name="motivo"]');
            
            // Verificar si el textarea está vacío
            if (motivoTextarea && motivoTextarea.value.trim() === '') {
                event.preventDefault();
                
                const alertDiv = document.createElement('div');
                alertDiv.className = 'alert alert-danger mt-2';
                alertDiv.textContent = 'Por favor, indica el motivo de la cancelación.';
                
                // Eliminar alertas previas
                const previousAlert = motivoTextarea.parentNode.querySelector('.alert');
                if (previousAlert) {
                    previousAlert.remove();
                }
                
                // Agregar la alerta después del textarea
                motivoTextarea.parentNode.appendChild(alertDiv);
                
                return false;
            }
        });
    }
    
    // Manejar el envío de formulario de rechazo
    if (rejectForm) {
        rejectForm.addEventListener('submit', function(event) {
            const motivoTextarea = this.querySelector('textarea[name="motivo"]');
            
            // Verificar si el textarea está vacío
            if (motivoTextarea && motivoTextarea.value.trim() === '') {
                event.preventDefault();
                
                const alertDiv = document.createElement('div');
                alertDiv.className = 'alert alert-danger mt-2';
                alertDiv.textContent = 'Por favor, indica el motivo del rechazo.';
                
                // Eliminar alertas previas
                const previousAlert = motivoTextarea.parentNode.querySelector('.alert');
                if (previousAlert) {
                    previousAlert.remove();
                }
                
                // Agregar la alerta después del textarea
                motivoTextarea.parentNode.appendChild(alertDiv);
                
                return false;
            }
        });
    }
    
    // Configurar botón de impresión
    const printBtn = document.querySelector('button[onclick="window.print()"]');
    if (printBtn) {
        printBtn.addEventListener('click', function(e) {
            e.preventDefault();
            preparePrintView();
            setTimeout(() => {
                window.print();
            }, 300);
        });
    }
    
    // Función para preparar la vista de impresión
    function preparePrintView() {
        // Ocultar elementos que no deben aparecer en la impresión
        const elementsToHide = document.querySelectorAll('.reservation-actions, .modal');
        elementsToHide.forEach(el => {
            el.setAttribute('data-print-hidden', 'true');
            el.style.display = 'none';
        });
        
        // Añadir el título de la reserva si no existe
        if (!document.getElementById('print-header')) {
            const printHeader = document.createElement('div');
            printHeader.id = 'print-header';
            printHeader.className = 'd-none d-print-block mb-4 text-center';
            
            // Logo y título (solo visible al imprimir)
            printHeader.innerHTML = `
                <h1 class="h3">HomeFlex - Comprobante de Reserva</h1>
                <p class="text-muted mb-0">Este documento sirve como comprobante oficial de su reserva.</p>
            `;
            
            const cardBody = document.querySelector('.card-body');
            if (cardBody) {
                cardBody.insertBefore(printHeader, cardBody.firstChild);
            }
        }
        
        // Restaurar visibilidad después de imprimir
        window.addEventListener('afterprint', function() {
            document.querySelectorAll('[data-print-hidden="true"]').forEach(el => {
                el.style.display = '';
                el.removeAttribute('data-print-hidden');
            });
        });
    }
    
    // Inicializar tooltips para fechas y estados
    initializeTooltips();
    
    // Calcular y mostrar duración de la estancia
    showStayDuration();
    
    // Función para inicializar tooltips
    function initializeTooltips() {
        const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
        if (tooltipTriggerList.length > 0 && typeof bootstrap !== 'undefined' && bootstrap.Tooltip) {
            tooltipTriggerList.map(function(tooltipTriggerEl) {
                return new bootstrap.Tooltip(tooltipTriggerEl);
            });
        }
    }
    
    // Función para calcular y mostrar la duración de la estancia
    function showStayDuration() {
        const fechaInicioEl = document.querySelector('.list-group-item span[th\\:text*="fechaInicio"]');
        const fechaFinEl = document.querySelector('.list-group-item span[th\\:text*="fechaFin"]');
        
        if (fechaInicioEl && fechaFinEl) {
            try {
                // Obtener fechas
                const fechaInicio = parseFechaEspanol(fechaInicioEl.textContent);
                const fechaFin = parseFechaEspanol(fechaFinEl.textContent);
                
                if (fechaInicio && fechaFin) {
                    // Calcular noches
                    const noches = calcularNoches(fechaInicio, fechaFin);
                    
                    // Actualizar información en el DOM si es necesario
                    const nochesPrevEl = document.querySelector('.list-group-item span:not([th\\:text])');
                    if (nochesPrevEl) {
                        nochesPrevEl.textContent = noches;
                    }
                    
                    // Añadir info sobre la duración
                    const listaEstancia = document.querySelector('.list-group');
                    if (listaEstancia && noches > 0) {
                        const diasText = noches === 1 ? 'día' : 'días';
                        const duracionText = `${noches} ${diasText} (${noches+1} noches)`;
                        
                        // Actualizar elemento existente o crear uno nuevo
                        let duracionEl = document.querySelector('.estancia-duracion');
                        if (!duracionEl) {
                            duracionEl = document.createElement('div');
                            duracionEl.className = 'estancia-duracion text-center mt-2 text-primary';
                            listaEstancia.parentNode.appendChild(duracionEl);
                        }
                        duracionEl.textContent = `Duración total: ${duracionText}`;
                    }
                }
            } catch (error) {
                console.error('Error al calcular duración:', error);
            }
        }
    }
    
    // Función auxiliar para parsear fecha en formato español (dd/mm/yyyy)
    function parseFechaEspanol(fechaStr) {
        if (!fechaStr) return null;
        
        const parts = fechaStr.trim().split('/');
        if (parts.length !== 3) return null;
        
        // Crear fecha en formato yyyy-mm-dd
        return new Date(parts[2], parts[1] - 1, parts[0]);
    }
    
    // Función auxiliar para calcular noches entre fechas
    function calcularNoches(fechaInicio, fechaFin) {
        if (!fechaInicio || !fechaFin) return 0;
        
        // Diferencia en milisegundos y convertir a días
        const diffTime = Math.abs(fechaFin - fechaInicio);
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        
        return diffDays;
    }
    
    // Agregar animación a la línea de tiempo
    animateTimeline();
    
    // Función para animar la línea de tiempo 
    function animateTimeline() {
        const timelineItems = document.querySelectorAll('.timeline-icon');
        
        timelineItems.forEach((item, index) => {
            // Añadir clase para animación con delay según posición
            setTimeout(() => {
                item.classList.add('animate__animated', 'animate__bounceIn');
            }, index * 300); // 300ms de delay entre cada item
        });
    }
    
    // Añadir handler para mostrar el mapa de ubicación
    setupMapModal();
    
    // Función para configurar modal del mapa
    function setupMapModal() {
        // Si existe un botón para ver mapa
        const mapBtn = document.querySelector('.btn-ver-mapa');
        if (mapBtn) {
            mapBtn.addEventListener('click', function(e) {
                e.preventDefault();
                
                // Si no existe el modal, crearlo
                if (!document.getElementById('mapModal')) {
                    const modalHtml = `
                        <div class="modal fade" id="mapModal" tabindex="-1" aria-labelledby="mapModalLabel" aria-hidden="true">
                            <div class="modal-dialog modal-lg">
                                <div class="modal-content">
                                    <div class="modal-header">
                                        <h5 class="modal-title" id="mapModalLabel">Ubicación de la propiedad</h5>
                                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                                    </div>
                                    <div class="modal-body">
                                        <div id="propertyMap" style="height: 400px;"></div>
                                    </div>
                                </div>
                            </div>
                        </div>
                    `;
                    
                    document.body.insertAdjacentHTML('beforeend', modalHtml);
                    
                    // Inicializar el mapa cuando el modal se abra
                    const mapModal = document.getElementById('mapModal');
                    mapModal.addEventListener('shown.bs.modal', function() {
                        initializeMap(mapBtn.dataset.lat, mapBtn.dataset.lon);
                    });
                }
                
                // Abrir el modal
                const mapModal = new bootstrap.Modal(document.getElementById('mapModal'));
                mapModal.show();
            });
        }
    }
    
    // Función para inicializar el mapa
    function initializeMap(lat, lon) {
        // Si no tenemos coordenadas o la API de mapas, salir
        if (!lat || !lon || !window.L) return;
        
        // Convertir a números
        lat = parseFloat(lat);
        lon = parseFloat(lon);
        
        // Inicializar mapa con leaflet
        const map = L.map('propertyMap').setView([lat, lon], 15);
        
        // Añadir capa de OpenStreetMap
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);
        
        // Añadir marcador
        L.marker([lat, lon]).addTo(map)
            .bindPopup('Ubicación de la propiedad')
            .openPopup();
    }
});