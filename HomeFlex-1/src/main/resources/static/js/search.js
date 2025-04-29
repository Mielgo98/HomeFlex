document.querySelector('#searchForm').addEventListener('submit', e => {
  const inDate = new Date(e.target.checkIn.value);
  const outDate = new Date(e.target.checkOut.value);
  if (outDate <= inDate) {
    e.preventDefault();
    alert('La fecha de salida debe ser posterior a la de entrada.');
  }
});