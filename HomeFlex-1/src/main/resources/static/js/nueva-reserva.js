// static/js/nueva-reserva.js

document.addEventListener('DOMContentLoaded', function() {
    // Referencias a elementos del DOM
    const fechaInicioInput = document.getElementById('fechaInicio');
    const fechaFinInput = document.getElementById('fechaFin');
    const numHuespedesSelect = document.getElementById('numHuespedes');
    const checkAvailabilityBtn = document.getElementById('checkAvailabilityBtn');
    const submitReservationBtn = document.getElementById('submitReservationBtn');
    const reservationSummary = document.getElementById('reservationSummary');
    const availabilityResult = document.getElementById('availabilityResult');
    const propiedadId = document.querySelector('input[name="propiedadId"]').value;
    
    // Variables para cálculos
    const precioDia = parseFloat(document.querySelector('.price-info .text-primary').textContent.replace('€', '').trim().replace(',', '.'));
    const precioSemanaEl = document.querySelector('.price-info .text-success');
    const precioSemana = precioSemanaEl ? parseFloat(precioSemanaEl.textContent.replace('€', '').trim().replace(',', '.')) : null;
    
    // Días no disponibles (se cargarán desde el servidor)
    let unavailableDates = [];
    
    // Establecer fecha mínima como hoy
    const today = new Date();
    const tomorrow = new Date(today);
    tomorrow.setDate(tomorrow.getDate() + 1);
    
    const formattedToday = today.toISOString().split('T')[0];
    const formattedTomorrow = tomorrow.toISOString().split('T')[0];
    
    fechaInicioInput.min = formattedToday;
    fechaFinInput.min = formattedTomorrow;
    
    // Cargar fechas no disponibles
    loadUnavailableDates();
    
    // Event listeners
    fechaInicioInput.addEventListener('change', handleFechaInicioChange);
    fechaFinInput.addEventListener('change', updateReservationSummary);
    numHuespedesSelect.addEventListener('change', updateReservationSummary);
    checkAvailabilityBtn.addEventListener('click', checkAvailability);
    
    // Inicializar calendario
    initializeCalendar();
    
    /**
     * Carga las fechas no disponibles para la propiedad
     */
    function loadUnavailableDates() {
        fetch(`/api/reservas/fechas-ocupadas/${propiedadId}`)
            .then(response => response.json())
            .then(data => {
                unavailableDates = data.map(dateStr => new Date(dateStr));
                console.log('Fechas no disponibles cargadas:', unavailableDates);
                initializeCalendar(); // Reinicializar calendario con las fechas cargadas
            })
            .catch(error => {
                console.error('Error al cargar fechas no disponibles:', error);
            });
    }
    
    /**
     * Maneja el cambio en la fecha de inicio
     */
    function handleFechaInicioChange() {
        const fechaInicio = new Date(fechaInicioInput.value);
        const minFechaFin = new Date(fechaInicio);
        minFechaFin.setDate(minFechaFin.getDate() + 1);
        
        fechaFinInput.min = minFechaFin.toISOString().split('T')[0];
        
        // Si la fecha de fin es anterior a la nueva fecha mínima, actualizarla
        if (fechaFinInput.value && new Date(fechaFinInput.value) < minFechaFin) {
            fechaFinInput.value = minFechaFin.toISOString().split('T')[0];
        }
        
        updateReservationSummary();
    }
    
    /**
     * Actualiza el resumen de la reserva
     */
    function updateReservationSummary() {
        if (fechaInicioInput.value && fechaFinInput.value && numHuespedesSelect.value) {
            const fechaInicio = new Date(fechaInicioInput.value);
            const fechaFin = new Date(fechaFinInput.value);
            const numHuespedes = parseInt(numHuespedesSelect.value);
            
            // Calcular número de noches
            const numNoches = Math.round((fechaFin - fechaInicio) / (1000 * 60 * 60 * 24));
            
            // Calcular precio
            let precioTotal = 0;
            
            if (precioSemana && numNoches >= 7) {
                const semanas = Math.floor(numNoches / 7);
                const diasRestantes = numNoches % 7;
                precioTotal = (semanas * precioSemana) + (diasRestantes * precioDia);
            } else {
                precioTotal = numNoches * precioDia;
            }
            
            // Actualizar el resumen
            document.getElementById('summaryDates').textContent = `${formatDate(fechaInicio)} - ${formatDate(fechaFin)}`;
            document.getElementById('summaryNights').textContent = `${numNoches} ${numNoches === 1 ? 'noche' : 'noches'}`;
            document.getElementById('summaryGuests').textContent = `${numHuespedes} ${numHuespedes === 1 ? 'huésped' : 'huéspedes'}`;
            document.getElementById('summaryBasePrice').textContent = `${precioDia.toFixed(2).replace('.', ',')} € x ${numNoches} noches`;
            document.getElementById('summaryTotal').textContent = `${precioTotal.toFixed(2).replace('.', ',')} €`;
            
            // Mostrar el resumen
            reservationSummary.style.display = 'block';
        } else {
            reservationSummary.style.display = 'none';
        }
    }
    
    /**
     * Verifica la disponibilidad para las fechas seleccionadas
     */
    function checkAvailability() {
        if (!fechaInicioInput.value || !fechaFinInput.value) {
            availabilityResult.innerHTML = `
                <div class="alert alert-warning">
                    <i class="bi bi-exclamation-triangle-fill me-2"></i>
                    Por favor, selecciona las fechas de llegada y salida.
                </div>
            `;
            availabilityResult.style.display = 'block';
            return;
        }
        
        // Mostrar indicador de carga
        availabilityResult.innerHTML = `
            <div class="text-center p-3">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Verificando disponibilidad...</span>
                </div>
                <p class="mt-2 mb-0">Verificando disponibilidad...</p>
            </div>
        `;
        availabilityResult.style.display = 'block';
        
        // Realizar la verificación con el servidor
        fetch(`/api/reservas/verificar-disponibilidad?propiedadId=${propiedadId}&fechaInicio=${fechaInicioInput.value}&fechaFin=${fechaFinInput.value}`)
            .then(response => response.json())
            .then(data => {
                if (data.data === true) {
                    // Disponible
                    availabilityResult.innerHTML = `
                        <div class="alert alert-success">
                            <i class="bi bi-check-circle-fill me-2"></i>
                            ¡Las fechas seleccionadas están disponibles! Puedes proceder con la reserva.
                        </div>
                    `;
                    // Habilitar botón de reserva
                    submitReservationBtn.disabled = false;
                } else {
                    // No disponible
                    availabilityResult.innerHTML = `
                        <div class="alert alert-danger">
                            <i class="bi bi-x-circle-fill me-2"></i>
                            Lo sentimos, las fechas seleccionadas no están disponibles. Por favor, selecciona otras fechas.
                        </div>
                    `;
                    // Mantener deshabilitado el botón de reserva
                    submitReservationBtn.disabled = true;
                }
            })
            .catch(error => {
                console.error('Error al verificar disponibilidad:', error);
                availabilityResult.innerHTML = `
                    <div class="alert alert-danger">
                        <i class="bi bi-exclamation-circle-fill me-2"></i>
                        Ocurrió un error al verificar la disponibilidad. Por favor, inténtalo de nuevo.
                    </div>
                `;
                submitReservationBtn.disabled = true;
            });
    }
    
    /**
     * Inicializa el calendario de disponibilidad
     */
    function initializeCalendar() {
        const calendarContainer = document.getElementById('availability-calendar');
        calendarContainer.innerHTML = ''; // Limpiar calendario existente
        
        // Generar 3 meses desde el mes actual
        const currentDate = new Date();
        
        for (let i = 0; i < 3; i++) {
            const targetDate = new Date(currentDate.getFullYear(), currentDate.getMonth() + i, 1);
            const monthCalendar = generateMonthCalendar(targetDate);
            calendarContainer.appendChild(monthCalendar);
        }
    }
    
    /**
     * Genera el calendario para un mes específico
     */
    function generateMonthCalendar(date) {
        const year = date.getFullYear();
        const month = date.getMonth();
        
        const monthElement = document.createElement('div');
        monthElement.className = 'calendar-month';
        
        // Título del mes
        const monthTitle = document.createElement('h5');
        monthTitle.className = 'month-title';
        monthTitle.textContent = new Date(year, month, 1).toLocaleDateString('es', { month: 'long', year: 'numeric' });
        monthElement.appendChild(monthTitle);
        
        // Crear grid del calendario
        const calendarGrid = document.createElement('div');
        calendarGrid.className = 'calendar-grid';
        
        // Agregar cabeceras de días
        const dayNames = ['Lun', 'Mar', 'Mié', 'Jue', 'Vie', 'Sáb', 'Dom'];
        dayNames.forEach(day => {
            const dayHeader = document.createElement('div');
            dayHeader.className = 'day-header';
            dayHeader.textContent = day;
            calendarGrid.appendChild(dayHeader);
        });
        
        // Determinar el primer día del mes (0 = Domingo, 1 = Lunes, etc.)
        const firstDay = new Date(year, month, 1).getDay();
        // Ajuste para empezar en lunes (convertir domingo de 0 a 6)
        const firstDayAdjusted = firstDay === 0 ? 6 : firstDay - 1;
        
        // Días del mes anterior para completar la primera semana
        const lastDayPrevMonth = new Date(year, month, 0).getDate();
        for (let i = 0; i < firstDayAdjusted; i++) {
            const dayElement = document.createElement('div');
            dayElement.className = 'calendar-day disabled';
            dayElement.textContent = lastDayPrevMonth - firstDayAdjusted + i + 1;
            calendarGrid.appendChild(dayElement);
        }
        
        // Días del mes actual
        const daysInMonth = new Date(year, month + 1, 0).getDate();
        const today = new Date();
        today.setHours(0, 0, 0, 0);
        
        for (let i = 1; i <= daysInMonth; i++) {
            const currentDate = new Date(year, month, i);
            const dayElement = document.createElement('div');
            dayElement.className = 'calendar-day';
            dayElement.textContent = i;
            
            // Determinar si el día está disponible
            const isUnavailable = unavailableDates.some(date => 
                date.getFullYear() === currentDate.getFullYear() && 
                date.getMonth() === currentDate.getMonth() && 
                date.getDate() === currentDate.getDate()
            );
            
            // Determinar si el día es pasado
            const isPast = currentDate < today;
            
            // Aplicar clases según disponibilidad
            if (isPast) {
                dayElement.classList.add('disabled');
            } else if (isUnavailable) {
                dayElement.classList.add('unavailable');
            } else {
                dayElement.classList.add('available');
                // Solo añadir evento de clic a días disponibles
				dayElement.addEventListener('click', () => {
				                    // Convertir la fecha seleccionada al formato YYYY-MM-DD
				                    const year = currentDate.getFullYear();
				                    const month = String(currentDate.getMonth() + 1).padStart(2, '0');
				                    const day = String(currentDate.getDate()).padStart(2, '0');
				                    const formattedDate = `${year}-${month}-${day}`;
				                    
				                    // Si no hay fecha de inicio seleccionada o ya hay fecha de fin seleccionada, establecer como fecha de inicio
				                    if (!fechaInicioInput.value || fechaFinInput.value) {
				                        fechaInicioInput.value = formattedDate;
				                        fechaFinInput.value = '';
				                        handleFechaInicioChange();
				                        updateCalendarSelection();
				                    } else {
				                        // De lo contrario, establecer como fecha de fin si es posterior a la fecha de inicio
				                        const fechaInicio = new Date(fechaInicioInput.value);
				                        if (currentDate > fechaInicio) {
				                            fechaFinInput.value = formattedDate;
				                            updateReservationSummary();
				                            updateCalendarSelection();
				                        }
				                    }
				                });
				            }
				            
				            calendarGrid.appendChild(dayElement);
				        }
				        
				        // Completar con los primeros días del mes siguiente si es necesario
				        const totalDaysDisplayed = firstDayAdjusted + daysInMonth;
				        const remainingCells = 42 - totalDaysDisplayed; // 6 filas de 7 días
				        
				        for (let i = 1; i <= remainingCells && i <= 7; i++) {
				            const dayElement = document.createElement('div');
				            dayElement.className = 'calendar-day disabled';
				            dayElement.textContent = i;
				            calendarGrid.appendChild(dayElement);
				        }
				        
				        monthElement.appendChild(calendarGrid);
				        return monthElement;
				    }
				    
				    /**
				     * Actualiza la selección visual en el calendario
				     */
				    function updateCalendarSelection() {
				        // Quitar todas las selecciones previas
				        document.querySelectorAll('.calendar-day.selected').forEach(day => {
				            day.classList.remove('selected');
				        });
				        
				        if (!fechaInicioInput.value) return;
				        
				        const fechaInicio = new Date(fechaInicioInput.value);
				        const fechaFin = fechaFinInput.value ? new Date(fechaFinInput.value) : null;
				        
				        // Seleccionar los días en el rango
				        document.querySelectorAll('.calendar-day:not(.disabled)').forEach(dayElement => {
				            const day = parseInt(dayElement.textContent);
				            const monthContainer = dayElement.closest('.calendar-month');
				            const monthTitle = monthContainer.querySelector('.month-title').textContent;
				            const [mes, ano] = monthTitle.split(' de ');
				            
				            // Convertir nombre del mes a número
				            const meses = ['enero', 'febrero', 'marzo', 'abril', 'mayo', 'junio', 'julio', 'agosto', 'septiembre', 'octubre', 'noviembre', 'diciembre'];
				            const mesNum = meses.findIndex(m => m === mes.toLowerCase());
				            
				            if (mesNum === -1) return;
				            
				            const currentDate = new Date(parseInt(ano), mesNum, day);
				            
				            // Verificar si está en el rango seleccionado
				            if (fechaFin) {
				                if (currentDate >= fechaInicio && currentDate <= fechaFin) {
				                    dayElement.classList.add('selected');
				                }
				            } else if (currentDate.getTime() === fechaInicio.getTime()) {
				                dayElement.classList.add('selected');
				            }
				        });
				    }
				    
				    /**
				     * Formatea una fecha en formato legible
				     */
				    function formatDate(date) {
				        const options = { weekday: 'short', day: 'numeric', month: 'short', year: 'numeric' };
				        return date.toLocaleDateString('es-ES', options);
				    }
				    
				    // Validar el formulario antes de enviar
				    document.getElementById('reservationForm').addEventListener('submit', function(event) {
				        if (!fechaInicioInput.value || !fechaFinInput.value || !numHuespedesSelect.value) {
				            event.preventDefault();
				            
				            availabilityResult.innerHTML = `
				                <div class="alert alert-warning">
				                    <i class="bi bi-exclamation-triangle-fill me-2"></i>
				                    Por favor, completa todos los campos obligatorios.
				                </div>
				            `;
				            availabilityResult.style.display = 'block';
				            
				            return false;
				        }
				        
				        if (submitReservationBtn.disabled) {
				            event.preventDefault();
				            
				            availabilityResult.innerHTML = `
				                <div class="alert alert-warning">
				                    <i class="bi bi-exclamation-triangle-fill me-2"></i>
				                    Por favor, verifica la disponibilidad antes de continuar.
				                </div>
				            `;
				            availabilityResult.style.display = 'block';
				            
				            return false;
				        }
				        
				        return true;
				    });
				});