window.Auth = (function(){
  const state = { user: null };

  async function getCurrentUser(){
    try{
      const res = await fetch('/api/auth/me', {credentials:'include'});
      if(!res.ok) return null;
      const user = await res.json();
      state.user = user;
      return user;
    }catch(e){ return null; }
  }

  function navHtml(){
    if(state.user){
      const u = state.user;
      return `
        <div class="nav-actions">
          <a class="nav-link" href="/analytics.html">Analytics</a>
          <a class="nav-link" href="/index.html">Home</a>
          <a class="nav-profile" href="/profile.html">
            <img class="avatar" src="${u.photoUrl||''}" alt="avatar"/>
            <span class="nav-username">${u.name||u.username}</span>
          </a>
          <button id="logout-btn" class="btn btn-outline">Logout</button>
        </div>`;
    }
    return `
      <div class="nav-actions">
        <a class="nav-link" href="/analytics.html">Analytics</a>
        <a class="nav-link" href="/index.html">Home</a>
        <a class="btn btn-primary" href="/login.html">Login</a>
      </div>`;
  }

  async function renderNav(){
    if(state.user==null){ await getCurrentUser(); }
    const nav = document.getElementById('nav-auth');
    if(!nav) return;
    nav.innerHTML = navHtml();
    const btn = document.getElementById('logout-btn');
    if(btn){
      btn.addEventListener('click', async ()=>{
        await fetch('/api/auth/logout', {method:'POST', credentials:'include'});
        state.user = null;
        window.location.href = '/login.html';
      });
    }
  }

  return { getCurrentUser, renderNav };
})();
