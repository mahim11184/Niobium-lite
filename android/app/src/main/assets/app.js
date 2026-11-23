const heroAddress=document.getElementById('hero-address');
const search=document.getElementById('hero-search');
function target(value){
  let v=(value||'').trim(); if(!v)return;
  if(!/^[a-zA-Z][a-zA-Z0-9+.-]*:/.test(v)){
    v=/^(localhost|\\d{1,3}(\\.\\d{1,3}){3}|[^\\s]+\\.[^\\s]+)$/.test(v)?'https://'+v:'https://www.google.com/search?q='+encodeURIComponent(v);
  }
  if(window.Niobium) Niobium.open(v);
}
search.addEventListener('submit',e=>{e.preventDefault();target(heroAddress.value);});
document.querySelectorAll('[data-url]').forEach(b=>b.addEventListener('click',()=>target(b.dataset.url)));