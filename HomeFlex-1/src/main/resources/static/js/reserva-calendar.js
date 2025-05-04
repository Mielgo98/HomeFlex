document.addEventListener('DOMContentLoaded', function() {
  const modalEl = document.getElementById('calendarioModal');
  let calendar; // lo mantendremos en closure

  modalEl.addEventListener('shown.bs.modal', function() {
    // Si ya existe, basta con renderizarlo de nuevo
    if (calendar) {
      calendar.render();
      return;
    }

    const calendarEl = document.getElementById('calendar');
    calendar = new FullCalendar.Calendar(calendarEl, {
      initialView: 'dayGridMonth',
      height: 600,
      selectable: false,
      headerToolbar: {
        left: 'prev,next today',
        center: 'title',
        right: ''
      },
      // fechas ocupadas vendrán como background events en rojo:
      events: function(fetchInfo, successCallback, failureCallback) {
        fetch(`/api/reservas/propiedad/${propiedadId}`)
          .then(resp => {
            if (!resp.ok) throw new Error('No se pudieron cargar reservas');
            return resp.json();
          })
          .then(reservas => {
            const eventos = reservas.map(r => ({
              start: r.fechaInicio,
              end: r.fechaFin,
              display: 'background',
              color: 'red'
            }));
            successCallback(eventos);
          })
          .catch(err => {
            console.error(err);
            failureCallback(err);
          });
      },
      // Opcional: días libres en verde
      dayCellDidMount: function(info) {
        // si no está marcado como ocupado, ponemos fondo verde claro
        const hasRed = calendar.getEvents().some(ev =>
          ev.display === 'background' &&
          info.date >= ev.start && info.date < ev.end
        );
        if (!hasRed) {
          info.el.style.backgroundColor = '#d4edda';
        }
      }
    });

    calendar.render();
  });
});
