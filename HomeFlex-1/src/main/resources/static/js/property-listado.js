document.addEventListener('DOMContentLoaded', () => {
  const prev = document.getElementById('prev');
  const next = document.getElementById('next');
  const params = new URLSearchParams(window.location.search);
  const size = params.get('size') || 9;
  let page = parseInt(params.get('pagina') || 0, 10);

  prev.addEventListener('click', () => navigate(page - 1));
  next.addEventListener('click', () => navigate(page + 1));

  function navigate(newPage) {
    window.location = `${window.location.pathname}?pagina=${newPage}&size=${size}`;
  }
});
