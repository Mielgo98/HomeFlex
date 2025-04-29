console.log("rualndo")

window.addEventListener('DOMContentLoaded', () => {
  const btn = document.querySelector('.menu-btn');
  const links = document.querySelector('.nav-links');
  btn.addEventListener('click', () => links.classList.toggle('open'));
});