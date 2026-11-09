import {escapeHTML as e, rupeesToPaise, money, niceDate, statusStyle, parseApiError} from "./core.mjs";

const root = document.querySelector("#app");
const notification = document.querySelector("#toast");
const API = String(window.WEDFLOW_API_BASE || "").replace(/\/+$/, "");
const store = {
  token: sessionStorage.getItem("wedflow_token") || "",
  me: null,
  weddings: [],
  weddingId: sessionStorage.getItem("wedflow_wedding") || "",
  vendorId: "",
  vendorsQuery: {category:"", city:""},
};
let toastTimer;

function toast(message, error = false) {
  notification.textContent = message;
  notification.classList.toggle("error", error);
  notification.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => { notification.hidden = true; }, 6200);
}
async function api(path, {method="GET", body} = {}) {
  let response;
  try {
    response = await fetch(API + "/api/v1" + path, {
      method,
      headers: {
        Accept: "application/json",
        ...(body === undefined ? {} : {"Content-Type": "application/json"}),
        ...(store.token ? {Authorization: "Bearer " + store.token} : {})
      },
      body: body === undefined ? undefined : JSON.stringify(body),
      cache: "no-store"
    });
  } catch {
    throw new Error("Cannot connect to WedFlow. Check your connection and try again.");
  }
  let result = null;
  if (response.status !== 204) {
    result = await response.json().catch(() => null);
  }
  if (!response.ok) {
    if (response.status === 401 && !path.startsWith("/auth/")) {
      store.token = "";
      store.me = null;
      sessionStorage.removeItem("wedflow_token");
    }
    throw parseApiError(response.status, result, response.headers.get("X-Request-ID"));
  }
  return result;
}
function form(id, handler) {
  const element = document.getElementById(id);
  if (!element) return;
  element.addEventListener("submit", async event => {
    event.preventDefault();
    const button = element.querySelector('[type="submit"]');
    if (button?.disabled) return;
    if (button) button.disabled = true;
    try {
      await handler(Object.fromEntries(new FormData(element)));
    } catch (error) {
      toast(error.message || "Unable to save the change.", true);
    } finally {
      if (button?.isConnected) button.disabled = false;
    }
  });
}
const button = (label, cls="btn") => '<button type="submit" class="' + cls + '">' + e(label) + '</button>';
const field = (label, name, type="text", options="", required=true) =>
  '<div class="field"><label for="' + e(name) + '">' + e(label) + '</label><input id="' + e(name) +
  '" name="' + e(name) + '" type="' + type + '" ' + options + (required ? ' required' : '') + '></div>';
const empty = (title, description) =>
  '<div class="empty"><div class="empty-icon" aria-hidden="true">✧</div><h3>' + e(title) +
  '</h3><p>' + e(description) + '</p></div>';
const badge = status => '<span class="tag ' + statusStyle(status) + '">' + e(status) + '</span>';
const item = (name, sub, right="") =>
  '<div class="list-row"><div><div class="row-title">' + e(name) +
  '</div><div class="row-sub">' + e(sub) + '</div></div><div class="row-actions">' + right + '</div></div>';
const routes = [
  ["overview","Overview","◈"],
  ["planning","Wedding planning","♡"],
  ["vendors","Find vendors","♢"],
  ["finance","Bookings & payments","₹"],
  ["vendor","Vendor workspace","▤"],
  ["notifications","Notifications","◎"],
  ["admin","Administration","⚙"]
];
const current = () => {
  const [route = "overview", tab = ""] = location.hash.replace(/^#\/?/, "").split("/");
  return {route, tab};
};
function navLink([route,label,icon],active) {
  return '<a class="nav-link ' + (active === route ? "active" : "") + '" href="#/' + route +
    '" aria-label="' + e(label) + '"><span class="nav-icon" aria-hidden="true">' + icon +
    '</span><span class="nav-text">' + e(label) + '</span></a>';
}
function shell(route) {
  const visible = routes.filter(x => x[0] !== "admin" || ["ADMIN","SUPER_ADMIN"].includes(store.me?.role));
  const selected = store.weddings.some(w=>w.id===store.weddingId) ? store.weddingId : "";
  const weddingOptions = store.weddings.map(w => '<option value="' + e(w.id) + '" ' +
    (w.id === selected ? "selected" : "") + '>' + e(w.title) + '</option>').join("");
  root.innerHTML = `<div class="app-shell">
    <aside class="sidebar">
      <div class="brand"><div class="brand-icon">W</div><div class="brand-text"><div class="brand-name">WedFlow</div><div class="brand-caption">Every detail, beautifully</div></div></div>
      <div><div class="side-section">Workspace</div><nav class="nav-list" aria-label="Main navigation">${visible.map(x=>navLink(x,route)).join("")}</nav></div>
      <div class="side-bottom"><strong>Made for your moment</strong><p>From the first idea to the final celebration.</p><button type="button" data-logout>Sign out</button></div>
    </aside>
    <div class="main">
      <header class="topbar">
        <div><div class="eyebrow">Wedding planning studio</div><strong class="serif" style="font-size:1.15rem">Your celebration, organized.</strong></div>
        <div class="topbar-right">
          ${store.weddings.length ? '<select class="top-select" aria-label="Selected wedding" id="wedding-picker"><option value="">Select wedding</option>' + weddingOptions + '</select>' : ''}
          <div class="user-pill"><span class="avatar">${e((store.me?.email || "W")[0].toUpperCase())}</span><span class="role-text">${e(store.me?.role || "MEMBER")}</span></div>
          <button class="btn sm secondary" type="button" data-logout title="Sign out">Exit</button>
        </div>
      </header>
      <main class="page" id="view" tabindex="-1"><div class="skeleton"></div></main>
    </div>
    <nav class="mobile-nav" aria-label="Mobile navigation">${visible.filter(x=>["overview","planning","vendors","finance","vendor"].includes(x[0])).map(x=>
      '<a class="' + (route===x[0]?'active':'') + '" href="#/' + x[0] + '"><span class="nav-icon">'+x[2]+'</span>'+e(x[1].replace("Wedding ","").replace(" & payments",""))+'</a>'
    ).join("")}</nav>
  </div>`;
  document.getElementById("wedding-picker")?.addEventListener("change", ev=>{
    store.weddingId = ev.target.value;
    sessionStorage.setItem("wedflow_wedding",store.weddingId);
    navigate();
  });
}
function paint(title,description,content,action="") {
  document.getElementById("view").innerHTML =
   '<div class="page-intro"><div><div class="eyebrow">WedFlow workspace</div><h1>' + e(title) +
   '</h1><p>' + e(description) + '</p></div><div class="actions">' + action +
   '</div></div>' + content;
}
function tabs(route,items,selected) {
  return '<div class="tabs" role="navigation" aria-label="Workspace sections">' +
    items.map(([key,label])=>'<a class="tab '+(key===selected?'active':'')+
      '" href="#/'+route+'/'+key+'">'+e(label)+'</a>').join("")+'</div>';
}
async function loadWeddings() {
  store.weddings = await api("/weddings");
  if(!store.weddings.some(w=>w.id===store.weddingId)) {
    store.weddingId = store.weddings[0]?.id || "";
    sessionStorage.setItem("wedflow_wedding",store.weddingId);
  }
}
function needWedding() {
  if (store.weddingId) return false;
  paint("Start planning","Create a wedding workspace first.",empty("Your story starts here","Open the overview and create a wedding to start managing ceremonies, vendors and guests."),
    '<a class="btn" href="#/overview">Create wedding</a>');
  return true;
}
async function registerOrLogin() {
  const register = current().route === "register";
  root.innerHTML = `<div class="login-page">
    <section class="login-art">
      <div class="brand"><span class="brand-icon">W</span><div><div class="brand-name">WedFlow</div><div class="brand-caption">Every detail, beautifully</div></div></div>
      <div><span class="eyebrow" style="color:#ecd0c9">Thoughtful planning begins here</span>
        <h1>Every beautiful story deserves an extraordinary beginning.</h1>
        <p>Bring your celebrations, people, budgets and trusted vendors together in one peaceful place.</p></div>
      <p class="login-quote">“The little moments make the biggest memories.”</p>
    </section>
    <section class="login-content"><div class="login-box">
      <div class="eyebrow">${register ? "Begin your journey" : "Welcome back"}</div>
      <h2>${register ? "Create your account" : "Sign in to WedFlow"}</h2>
      <p class="muted">${register ? "A more joyful way to plan starts today." : "Your next beautiful detail is waiting."}</p>
      <form id="auth-form">
        <div class="field"><label for="email">Email address</label><input id="email" name="email" type="email" autocomplete="email" maxlength="254" required></div>
        <div class="field"><label for="password">Password</label><input id="password" name="password" type="password" minlength="12" maxlength="128" autocomplete="${register?"new-password":"current-password"}" required></div>
        <div class="form-actions">${button(register?"Create account":"Sign in")}</div>
      </form>
      <div class="switch">${register ? 'Already have an account? <a href="#/login">Sign in</a>' : 'New to WedFlow? <a href="#/register">Create an account</a>'}</div>
      <div class="footer-note">Secure access · No payment details stored in your browser</div>
    </div></section>
  </div>`;
  form("auth-form",async values=>{
    if(register){
      await api("/auth/register",{method:"POST",body:values});
      toast("Account created. Please sign in.");
      location.hash="#/login";
      return;
    }
    const login = await api("/auth/login",{method:"POST",body:values});
    store.token=login.accessToken;
    sessionStorage.setItem("wedflow_token",store.token);
    store.me=await api("/account/me");
    await loadWeddings();
    location.hash="#/overview";
    await navigate();
  });
}
async function overview(){
  const weddings=store.weddings;
  const total=weddings.reduce((sum,w)=>sum+Number(w.totalBudgetPaise||0),0);
  const soon=weddings.filter(w=>w.eventDate>=new Date().toISOString().slice(0,10)).sort((a,b)=>a.eventDate.localeCompare(b.eventDate));
  const cards = weddings.map(w=>`<article class="wedding-tile">
    <div class="eyebrow">Your celebration</div><h3>${e(w.title)}</h3>
    <div class="tile-meta"><span>⌖ ${e(w.city)}</span><span>◷ ${e(niceDate(w.eventDate))}</span><span>◈ ${e(money(w.totalBudgetPaise))} budget</span></div>
    <button type="button" class="btn secondary sm" data-choose="${e(w.id)}">Open workspace</button>
  </article>`).join("");
  const hero=`<section class="hero"><div><div class="eyebrow" style="color:#f3d0d8">Your personal planning studio</div>
    <h2>Make room for the beautiful moments.</h2>
    <p>Keep your wedding vision, schedule, spending and favourite vendors effortlessly in sync.</p></div>
    <a href="#/planning/ceremonies" class="btn">Open my planner →</a></section>`;
  paint("Your celebrations","A clear view of everything you're planning.",hero+
    `<div class="grid grid-4" style="margin-bottom:27px">
      <div class="metric"><div class="metric-label">Weddings</div><div class="metric-value">${weddings.length}</div><div class="small muted">Active workspaces</div></div>
      <div class="metric"><div class="metric-label">Total planned budget</div><div class="metric-value" style="font-size:1.45rem">${e(money(total))}</div><div class="small muted">Across your weddings</div></div>
      <div class="metric"><div class="metric-label">Upcoming</div><div class="metric-value">${soon.length}</div><div class="small muted">Scheduled celebrations</div></div>
      <div class="metric"><div class="metric-label">Next milestone</div><div class="metric-value" style="font-size:1rem;margin-top:16px">${e(soon[0]?niceDate(soon[0].eventDate):"Not scheduled")}</div></div>
    </div>
    <div class="card-title"><h2>Your wedding workspaces</h2></div>
    ${weddings.length?'<div class="grid grid-3">'+cards+'</div>':empty("No weddings created yet","Use the form below to create your first wedding workspace.")}
    <div class="panel" style="margin-top:26px"><div class="card-title"><h2>Create a wedding</h2><span class="tag">New workspace</span></div>
      <form id="new-wedding"><div class="form-grid">
        ${field("Wedding title","title","text",'maxlength="200" placeholder="Ananya & Kiran"')}
        ${field("City","city","text",'maxlength="120" placeholder="Bengaluru"')}
        ${field("Event date","eventDate","date")}
        ${field("Total budget (₹)","totalBudget","number",'min="1" step="0.01" placeholder="750000"')}
      </div><div class="form-actions">${button("Create workspace")}</div></form>
    </div>`);
  form("new-wedding",async v=>{
    const wedding=await api("/weddings",{method:"POST",body:{
      title:v.title,city:v.city,eventDate:v.eventDate,totalBudgetPaise:rupeesToPaise(v.totalBudget)}});
    await loadWeddings();store.weddingId=wedding.id;sessionStorage.setItem("wedflow_wedding",wedding.id);
    toast("Wedding workspace created.");location.hash="#/planning/ceremonies";await navigate();
  });
}
async function planning(tab="ceremonies"){
  if(needWedding())return;
  const id=encodeURIComponent(store.weddingId);
  const valid=["ceremonies","guests","tasks","budget","catering"];
  if(!valid.includes(tab))tab="ceremonies";
  const nav=tabs("planning",valid.map(x=>[x,x[0].toUpperCase()+x.slice(1)]),tab);
  let content="", binder=null;
  if(tab==="ceremonies"){
    const rows=await api("/weddings/"+id+"/ceremonies");
    content=`<div class="grid grid-2"><section class="panel"><h2>Celebration schedule</h2>
       ${rows.length?'<div class="timeline">'+rows.map(x=>'<div class="timeline-item"><strong>'+e(x.name)+'</strong><div class="row-sub">'+e(niceDate(x.eventDate))+' · '+e(x.venue||"Venue TBD")+'</div></div>').join("")+'</div>':empty("No ceremonies yet","Add your first ceremony or event to start the timeline.")}</section>
       <section class="panel"><h2>Add a ceremony</h2><form id="ceremony"><div class="form-grid">
       ${field("Ceremony name","name","text",'maxlength="120" placeholder="Reception"')}
       ${field("Date","eventDate","date")}
       ${field("Venue / location","venue","text",'maxlength="250" placeholder="Venue name"',false)}
       </div><div class="form-actions">${button("Add ceremony")}</div></form></section></div>`;
    binder=()=>form("ceremony",async v=>{await api("/weddings/"+id+"/ceremonies",{method:"POST",body:v});toast("Ceremony added.");await navigate();});
  } else if(tab==="guests"){
    const rows=await api("/weddings/"+id+"/guests");
    content=`<div class="grid grid-2"><section class="panel"><h2>Guest list <span class="tag">${rows.length}</span></h2>
       ${rows.length?'<div class="list">'+rows.map(x=>item(x.fullName,x.phone||"No phone",'<select aria-label="RSVP status for '+e(x.fullName)+'" data-rsvp="'+e(x.id)+'">'+["PENDING","ACCEPTED","DECLINED"].map(s=>'<option '+(s===x.rsvp?'selected':'')+'>'+s+'</option>').join("")+'</select>')).join("")+'</div>':empty("Your guest list is empty","Add guests and track invitations and RSVP status.")}</section>
       <section class="panel"><h2>Add guest</h2><form id="guest"><div class="stack">
       ${field("Full name","fullName","text",'maxlength="160" placeholder="Guest name"')}
       ${field("Phone number (optional)","phone","tel",'maxlength="30" placeholder="Optional"',false)}
       </div><div class="form-actions">${button("Add guest")}</div></form></section></div>`;
    binder=()=>form("guest",async v=>{await api("/weddings/"+id+"/guests",{method:"POST",body:{fullName:v.fullName,phone:v.phone||null}});toast("Guest added.");await navigate();});
  } else if(tab==="tasks"){
    const rows=await api("/weddings/"+id+"/tasks");
    content=`<div class="grid grid-2"><section class="panel"><h2>Planning checklist</h2>
      ${rows.length?'<div class="list">'+rows.map(x=>item(x.title,x.dueDate?niceDate(x.dueDate):"No due date",badge(x.completed?"COMPLETED":"PENDING")+(x.completed?'':'<button class="btn sm secondary" type="button" data-task="'+e(x.id)+'">Mark done</button>'))).join("")+'</div>':empty("No tasks yet","Keep every planning milestone in one checklist.")}</section>
      <section class="panel"><h2>New task</h2><form id="task"><div class="stack">
      ${field("Task title","title","text",'maxlength="200" placeholder="Confirm photographer"')}
      ${field("Due date (optional)","dueDate","date","",false)}
      </div><div class="form-actions">${button("Add task")}</div></form></section></div>`;
    binder=()=>form("task",async v=>{await api("/weddings/"+id+"/tasks",{method:"POST",body:{title:v.title,dueDate:v.dueDate||null}});toast("Task added.");await navigate();});
  } else if(tab==="budget"){
    const rows=await api("/weddings/"+id+"/budget");
    const wedding=store.weddings.find(x=>x.id===store.weddingId);
    const allocated=rows.reduce((sum,x)=>sum+Number(x.allocatedPaise),0);
    const available=(wedding?.totalBudgetPaise||0)-allocated;
    content=`<div class="grid grid-3" style="margin-bottom:20px">
      <div class="metric"><div class="metric-label">Total budget</div><div class="metric-value">${e(money(wedding?.totalBudgetPaise))}</div></div>
      <div class="metric"><div class="metric-label">Allocated</div><div class="metric-value">${e(money(allocated))}</div></div>
      <div class="metric"><div class="metric-label">Unallocated</div><div class="metric-value">${e(money(available))}</div></div>
    </div><div class="grid grid-2"><section class="panel"><h2>Budget categories</h2>
      ${rows.length?'<div class="list">'+rows.map(x=>item(x.name,"Spent: "+money(x.spentPaise),'<strong>'+e(money(x.allocatedPaise))+'</strong>')).join("")+'</div>':empty("No categories yet","Create allocations for catering, decoration and other expenses.")}</section>
      <section class="panel"><h2>Allocate budget</h2><form id="budget"><div class="stack">
      ${field("Category name","name","text",'maxlength="100" placeholder="Decorations"')}
      ${field("Allocation (₹)","amount","number",'min="0" step="0.01" placeholder="50000"')}
      </div><div class="form-actions">${button("Add allocation")}</div></form></section></div>`;
    binder=()=>form("budget",async v=>{await api("/weddings/"+id+"/budget",{method:"POST",body:{name:v.name,allocatedPaise:rupeesToPaise(v.amount)}});toast("Budget allocated.");await navigate();});
  } else {
    const menus=await api("/weddings/"+id+"/catering/menus");
    const expanded=await Promise.all(menus.map(async menu=>({
      ...menu,items:await api("/weddings/"+id+"/catering/menus/"+encodeURIComponent(menu.id)+"/items")
    })));
    content=`<div class="stack"><section class="panel"><h2>Menus & dietary needs</h2>
      ${expanded.length?'<div class="grid grid-2">'+expanded.map(m=>'<div class="wedding-tile"><h3>'+e(m.meal)+'</h3><div class="row-sub">'+e(m.guestCount)+' guests</div><div class="list" style="margin-top:14px">'+(m.items.length?m.items.map(x=>item(x.name,x.diet,'<span class="tag">'+e(x.quantity)+' portions</span>')).join(""):'<p class="muted small">No menu items yet</p>')+'</div></div>').join("")+'</div>':empty("No menus yet","Create a meal plan for your ceremony guests.")}</section>
      <div class="grid grid-2"><section class="panel"><h2>Create menu</h2><form id="menu"><div class="stack">
       ${field("Meal name","meal","text",'maxlength="30" placeholder="Reception dinner"')}
       ${field("Guest count","guestCount","number",'min="1" step="1" placeholder="200"')}
       </div><div class="form-actions">${button("Create menu")}</div></form></section>
       <section class="panel"><h2>Add menu item</h2>${menus.length?'<form id="menu-item"><div class="stack"><div class="field"><label for="menuId">Menu</label><select id="menuId" name="menuId" required>'+menus.map(x=>'<option value="'+e(x.id)+'">'+e(x.meal)+'</option>').join("")+'</select></div>'+field("Dish name","name","text",'maxlength="150" placeholder="Vegetable pulao"')+'<div class="field"><label for="diet">Diet</label><select name="diet" id="diet"><option value="VEG">Vegetarian</option><option value="NON_VEG">Non-vegetarian</option><option value="JAIN">Jain</option></select></div>'+field("Quantity","quantity","number",'min="1" step="1"')+'</div><div class="form-actions">'+button("Add item")+'</div></form>':empty("Create a menu first","Menu items belong to a meal plan.")}</section></div></div>`;
    binder=()=>{
      form("menu",async v=>{await api("/weddings/"+id+"/catering/menus",{method:"POST",body:{meal:v.meal,guestCount:Number(v.guestCount),ceremonyId:null}});toast("Menu created.");await navigate();});
      form("menu-item",async v=>{await api("/weddings/"+id+"/catering/menus/"+encodeURIComponent(v.menuId)+"/items",{method:"POST",body:{name:v.name,diet:v.diet,quantity:Number(v.quantity)}});toast("Menu item added.");await navigate();});
    };
  }
  const wedding=store.weddings.find(w=>w.id===store.weddingId);
  paint("Wedding planning", (wedding?.title||"Your wedding")+" · "+(wedding?.city||""),nav+content);
  binder?.();
}
async function vendors(){
  const {category,city}=store.vendorsQuery;
  const params=new URLSearchParams();
  if(category)params.set("category",category);
  if(city)params.set("city",city);
  const rows=await api("/vendors"+(params.size?"?"+params:""));
  const cards=rows.map(v=>`<article class="panel">
    <div style="display:flex;align-items:center;gap:13px;margin-bottom:15px"><div class="kpi-icon">✿</div><div><h3 style="margin:0">${e(v.displayName)}</h3><div class="row-sub">${e(v.category)} · ${e(v.city)}</div></div></div>
    <span class="tag ok">Approved vendor</span>
    ${store.weddingId?'<form data-enquiry-form="'+e(v.id)+'" style="margin-top:20px"><div class="field"><label for="message-'+e(v.id)+'">Your enquiry</label><textarea id="message-'+e(v.id)+'" name="message" maxlength="4000" placeholder="Share your event date, guest count and requirements" required></textarea></div><div class="form-actions">'+button("Send enquiry","btn sm")+'</div></form>':'<p class="muted small" style="margin-top:18px">Create a wedding before sending enquiries.</p>'}
  </article>`).join("");
  paint("Discover trusted vendors","Explore approved service providers and request tailored quotations.",
    `<div class="panel" style="margin-bottom:20px"><form id="vendor-search" class="toolbar" style="margin:0"><div class="field"><label for="category">Category</label><input id="category" name="category" value="${e(category)}" placeholder="Catering, photography…" maxlength="80"></div>
      <div class="field"><label for="city">City</label><input id="city" name="city" value="${e(city)}" placeholder="Bengaluru" maxlength="120"></div>
      <div class="form-actions">${button("Search vendors")}</div></form></div>
    ${rows.length?'<div class="grid grid-3">'+cards+'</div>':empty("No matching vendors","Try another category or city. Only approved vendors appear here.")}`);
  form("vendor-search",async v=>{store.vendorsQuery={category:v.category.trim(),city:v.city.trim()};await navigate();});
  document.querySelectorAll("[data-enquiry-form]").forEach(el=>{
    el.addEventListener("submit",async ev=>{
      ev.preventDefault();
      const b=el.querySelector('[type="submit"]');if(b.disabled)return;b.disabled=true;
      try {
        await api("/weddings/"+encodeURIComponent(store.weddingId)+"/enquiries",{method:"POST",body:{
          vendorId:el.getAttribute("data-enquiry-form"),message:new FormData(el).get("message")}});
        toast("Enquiry sent. Track replies under Bookings & payments.");
        el.reset();
      } catch(err){toast(err.message,true);} finally{b.disabled=false;}
    });
  });
}
async function finance(tab="enquiries"){
  if(needWedding())return;
  const id=encodeURIComponent(store.weddingId);
  const valid=["enquiries","quotes","bookings","payments"];
  if(!valid.includes(tab))tab="enquiries";
  const nav=tabs("finance",valid.map(x=>[x,x[0].toUpperCase()+x.slice(1)]),tab);
  let content="";
  if(tab==="enquiries"){
    const rows=await api("/weddings/"+id+"/enquiries");
    content='<section class="panel"><h2>Vendor enquiries</h2>'+
      (rows.length?'<div class="list">'+rows.map(x=>item(x.message,"Vendor reference: "+x.vendorId,badge(x.status))).join("")+'</div>':empty("No enquiries sent","Visit Find vendors to contact trusted providers."))+'</section>';
  } else if(tab==="quotes"){
    const rows=await api("/weddings/"+id+"/quotes");
    content='<section class="panel"><h2>Received quotations</h2>'+
      (rows.length?'<div class="list">'+rows.map(x=>item(money(x.amountPaise),"Vendor: "+x.vendorId,badge(x.status)+(x.status==="OFFERED"?'<form data-accept="'+e(x.id)+'" style="display:flex;gap:8px;flex-wrap:wrap;align-items:center"><input aria-label="Service date" type="date" name="serviceDate" min="'+new Date().toISOString().slice(0,10)+'" required>'+button("Accept quote","btn sm")+'</form>':""))).join("")+'</div>':empty("No quotations yet","Your vendor responses will appear once quotations are submitted."))+'</section>';
  } else if(tab==="bookings"){
    const rows=await api("/weddings/"+id+"/bookings");
    content='<section class="panel"><h2>Confirmed bookings</h2>'+
      (rows.length?'<div class="list">'+rows.map(x=>item("Booking "+x.id.slice(0,8),"Vendor reference: "+x.vendorId,badge(x.status)+(x.status==="CONFIRMED"?'<button type="button" class="btn sm danger" data-cancel="'+e(x.id)+'">Cancel</button><form data-intent="'+e(x.id)+'" style="display:flex;align-items:center;gap:7px"><input type="number" aria-label="Payment amount in rupees" name="amount" min="0.01" step="0.01" placeholder="Amount ₹" required style="max-width:140px">'+button("Create payment","btn sm")+'</form>':""))).join("")+'</div>':empty("No bookings yet","Accept a vendor quotation to confirm your booking."))+'</section>';
  } else {
    const rows=await api("/weddings/"+id+"/payments");
    content=`<div class="alert" style="margin-bottom:18px">A checkout request does not confirm payment. Only server-verified Razorpay capture changes status to <strong>VERIFIED</strong>. Refunds may remain pending until a verified provider callback arrives.</div>
      <section class="panel"><h2>Payment records</h2>
      ${rows.length?'<div class="list">'+rows.map(x=>item(money(x.amountPaise),"Booking: "+x.bookingId,badge(x.status)+(x.status==="PENDING"?'<button type="button" data-checkout="'+e(x.id)+'" class="btn sm">Pay securely</button>':"")+(x.status==="VERIFIED"?'<form data-refund="'+e(x.id)+'" style="display:flex;gap:7px;align-items:center"><input type="number" name="amount" aria-label="Refund amount in rupees" min="0.01" step="0.01" placeholder="Refund ₹" required style="max-width:130px">'+button("Request refund","btn sm secondary")+'</form>':""))).join("")+'</div>':empty("No payment records","Create a payment from a confirmed booking to get started.")}</section>`;
  }
  paint("Bookings & payments","Review enquiries, quotations, bookings and financial status.",nav+content);
  document.querySelectorAll("[data-accept]").forEach(el=>el.addEventListener("submit",async ev=>{
    ev.preventDefault();const btn=el.querySelector("button");btn.disabled=true;
    try{const date=new FormData(el).get("serviceDate");await api("/weddings/"+id+"/quotes/"+encodeURIComponent(el.dataset.accept)+"/accept?serviceDate="+encodeURIComponent(date),{method:"POST"});toast("Booking confirmed.");location.hash="#/finance/bookings";await navigate();}
    catch(err){toast(err.message,true);}finally{btn.disabled=false;}
  }));
  document.querySelectorAll("[data-intent]").forEach(el=>el.addEventListener("submit",async ev=>{
    ev.preventDefault();const btn=el.querySelector("button");btn.disabled=true;
    try{await api("/weddings/"+id+"/payments",{method:"POST",body:{bookingId:el.dataset.intent,amountPaise:rupeesToPaise(new FormData(el).get("amount"))}});toast("Payment intent created. Payment is not yet completed.");location.hash="#/finance/payments";await navigate();}
    catch(err){toast(err.message,true);}finally{btn.disabled=false;}
  }));
  document.querySelectorAll("[data-refund]").forEach(el=>el.addEventListener("submit",async ev=>{
    ev.preventDefault();if(!confirm("Request a refund? The provider processes this asynchronously."))return;
    const btn=el.querySelector("button");btn.disabled=true;
    try{await api("/weddings/"+id+"/refunds",{method:"POST",body:{paymentId:el.dataset.refund,amountPaise:rupeesToPaise(new FormData(el).get("amount"))}});toast("Refund requested; processing is pending provider confirmation.");await navigate();}
    catch(err){toast(err.message,true);}finally{btn.disabled=false;}
  }));
}
async function loadRazorpay() {
  if(window.Razorpay)return;
  await new Promise((resolve,reject)=>{
    const s=document.createElement("script");
    s.src="https://checkout.razorpay.com/v1/checkout.js";s.async=true;
    s.onload=resolve;s.onerror=()=>reject(new Error("Could not load secure checkout. Try again later."));
    document.head.appendChild(s);
  });
}
async function checkout(paymentId) {
  const details=await api("/razorpay/orders",{method:"POST",body:{paymentIntentId:paymentId}});
  await loadRazorpay();
  const modal=new window.Razorpay({
    key:details.keyId,order_id:details.orderId,amount:details.amountPaise,currency:details.currency,
    name:"WedFlow",description:"Wedding booking payment",
    handler:()=>{toast("Checkout returned. Awaiting verified payment confirmation; refresh the payment list.");navigate();},
    modal:{ondismiss:()=>toast("Checkout closed. Check payment status before retrying.")}
  });
  modal.open();
}
async function vendorWorkspace(tab="services"){
  const profiles=await api("/vendor-profiles/mine");
  if(!profiles.length){
    paint("Become a WedFlow vendor","Create your professional profile and request approval.",
      `<div class="grid grid-2"><section class="panel"><h2>Vendor registration</h2>
       <form id="vendor-register"><div class="stack">
       ${field("Business name","displayName","text",'maxlength="200" placeholder="Your business"')}
       ${field("Service category","category","text",'maxlength="80" placeholder="Catering"')}
       ${field("Operating city","city","text",'maxlength="120" placeholder="Bengaluru"')}
       <div class="field"><label for="description">About your services</label><textarea name="description" id="description" maxlength="2000" placeholder="Describe what you offer"></textarea></div>
       </div><div class="form-actions">${button("Request approval")}</div></form>
       </section><section class="panel subtle"><h2>Build trust with couples</h2><p class="muted">Your vendor listing will be visible after administrator approval. You can then respond to enquiries and manage your service calendar.</p></section></div>`);
    form("vendor-register",async v=>{await api("/vendor-profiles",{method:"POST",body:v});toast("Vendor profile submitted for approval.");await navigate();});
    return;
  }
  if(!profiles.some(p=>p.id===store.vendorId))store.vendorId=profiles[0].id;
  const profile=profiles.find(p=>p.id===store.vendorId);
  const valid=["services","enquiries","quotes","bookings"];
  if(!valid.includes(tab))tab="services";
  const id=encodeURIComponent(store.vendorId);
  const nav=tabs("vendor",valid.map(x=>[x,x[0].toUpperCase()+x.slice(1)]),tab);
  let content="";
  if(tab==="services"){
    const services=await api("/vendor-profiles/"+id+"/services");
    content=`<div class="grid grid-2"><section class="panel"><h2>My services</h2>
      ${services.length?'<div class="list">'+services.map(x=>item(x.title,money(x.basePricePaise),badge(x.active?"ACTIVE":"INACTIVE"))).join("")+'</div>':empty("No services yet","Add a service to build out your vendor profile.")}</section>
      <section class="panel"><h2>Add service</h2><form id="service"><div class="stack">
      ${field("Service title","title","text",'maxlength="200" placeholder="Premium decoration"')}
      ${field("Starting price (₹)","basePrice","number",'min="0" step="0.01" placeholder="10000"')}
      </div><div class="form-actions">${button("Add service")}</div></form></section></div>`;
  } else if(tab==="enquiries"){
    const rows=await api("/vendor-profiles/"+id+"/enquiries");
    content='<section class="panel"><h2>Customer enquiries</h2>'+
      (rows.length?'<div class="list">'+rows.map(x=>'<div class="list-row" style="display:block"><div class="row-title">'+e(x.weddingTitle)+'</div><p class="row-sub">'+e(x.message)+'</p>'+badge(x.status)+'<form data-quote="'+e(x.id)+'" style="margin-top:14px"><div class="form-grid">'+field("Your quote (₹)","amount-"+x.id,"number",'min="0.01" step="0.01" placeholder="15000"')+'<div class="field"><label>Quote description</label><textarea name="description" maxlength="2000" placeholder="Describe inclusions and deliverables"></textarea></div></div><div class="form-actions">'+button("Send quotation","btn sm")+'</div></form></div>').join("")+'</div>':empty("No enquiries yet","New customer requests for your services will appear here."))+'</section>';
  } else if(tab==="quotes"){
    const rows=await api("/vendor-profiles/"+id+"/quotes");
    content='<section class="panel"><h2>Sent quotations</h2>'+(rows.length?'<div class="list">'+rows.map(x=>item(money(x.amountPaise),x.description||"No description",badge(x.status))).join("")+'</div>':empty("No quotations yet","Respond to customer enquiries with a custom quote."))+'</section>';
  } else {
    const rows=await api("/vendor-profiles/"+id+"/bookings");
    content='<section class="panel"><h2>Vendor bookings</h2>'+(rows.length?'<div class="list">'+rows.map(x=>item("Booking "+x.id.slice(0,8),niceDate(x.serviceDate),badge(x.status))).join("")+'</div>':empty("No bookings yet","Accepted quotations will appear as bookings."))+'</section>';
  }
  paint("Vendor workspace",profile.displayName+" · "+profile.city,
    '<div class="alert" style="margin-bottom:19px">Profile status: '+badge(profile.status)+'. Public marketplace visibility requires approval.</div>'+
    (profiles.length>1?'<div class="toolbar"><label for="profile-picker">Choose profile</label><select id="profile-picker" class="top-select">'+profiles.map(x=>'<option value="'+e(x.id)+'" '+(x.id===store.vendorId?'selected':'')+'>'+e(x.displayName)+'</option>').join("")+'</select></div>':"")+
    nav+content);
  document.getElementById("profile-picker")?.addEventListener("change",ev=>{store.vendorId=ev.target.value;navigate();});
  form("service",async v=>{await api("/vendor-profiles/"+id+"/services",{method:"POST",body:{title:v.title,basePricePaise:rupeesToPaise(v.basePrice)}});toast("Service added.");await navigate();});
  document.querySelectorAll("[data-quote]").forEach(el=>el.addEventListener("submit",async ev=>{
    ev.preventDefault();const btn=el.querySelector("button");btn.disabled=true;
    try{const amount=el.querySelector('input[type="number"]').value;const description=el.querySelector("textarea").value;
      await api("/vendor-profiles/"+id+"/quotes",{method:"POST",body:{enquiryId:el.dataset.quote,amountPaise:rupeesToPaise(amount),description}});
      toast("Quotation sent.");location.hash="#/vendor/quotes";await navigate();
    }catch(err){toast(err.message,true);}finally{btn.disabled=false;}
  }));
}
async function notifications(){
  const rows=await api("/notifications");
  paint("Notifications","Updates that matter to your wedding journey.",
    '<section class="panel"><h2>Your inbox</h2>'+
    (rows.length?'<div class="list">'+rows.map(x=>item(x.title,x.body,badge(x.kind)+(x.read?"":'<button type="button" class="btn sm secondary" data-read="'+e(x.id)+'">Mark read</button>'))).join("")+'</div>':empty("All caught up","New updates about your wedding will appear here."))+'</section>');
}
async function administration(){
  if(!["ADMIN","SUPER_ADMIN"].includes(store.me?.role)){
    paint("Access restricted","This workspace is for administrators only.",empty("Permission required","Please use the appropriate authorized account."));
    return;
  }
  const rows=await api("/admin/vendors/pending");
  paint("Administration","Review and approve service providers before they appear in the marketplace.",
    '<section class="panel"><div class="card-title"><h2>Pending vendor reviews</h2><span class="tag">'+rows.length+' awaiting</span></div>'+
    (rows.length?'<div class="list">'+rows.map(x=>item(x.displayName,x.category+" · "+x.city+" · "+(x.description||"No description"),
      '<button type="button" class="btn sm" data-vendor-approve="'+e(x.id)+'">Approve</button><button type="button" class="btn sm danger" data-vendor-reject="'+e(x.id)+'">Reject</button>')).join("")+'</div>':empty("Approval queue is clear","There are no vendor profiles awaiting review."))+'</section>');
}
async function navigate(){
  const {route,tab}=current();
  if(!store.token){
    await registerOrLogin();
    return;
  }
  if(!store.me) {
    try{store.me=await api("/account/me");await loadWeddings();}
    catch(err){store.token="";sessionStorage.removeItem("wedflow_token");toast(err.message,true);location.hash="#/login";await registerOrLogin();return;}
  }
  shell(route);
  try{
    if(route==="overview")await overview();
    else if(route==="planning")await planning(tab);
    else if(route==="vendors")await vendors();
    else if(route==="finance")await finance(tab);
    else if(route==="vendor")await vendorWorkspace(tab);
    else if(route==="notifications")await notifications();
    else if(route==="admin")await administration();
    else{location.hash="#/overview";return;}
  }catch(err){
    const message=err.message || "Unable to load this page.";
    paint("Unable to load","Your data has not been changed.",
      '<div class="alert error">'+e(message)+'</div><div class="form-actions"><button type="button" data-retry class="btn">Retry</button></div>');
    if(!store.token){location.hash="#/login";await registerOrLogin();}
  }
}
root.addEventListener("click",async ev=>{
  const target=ev.target.closest("[data-logout],[data-choose],[data-task],[data-cancel],[data-checkout],[data-read],[data-vendor-approve],[data-vendor-reject],[data-retry]");
  if(!target)return;
  const action=target.dataset;
  target.disabled=true;
  try{
    if("logout" in action){
      try{await api("/auth/logout",{method:"POST"});}catch{ /* revoke local access even if network is down */ }
      store.token="";store.me=null;store.weddings=[];sessionStorage.removeItem("wedflow_token");sessionStorage.removeItem("wedflow_wedding");
      location.hash="#/login";await navigate();return;
    }
    if(action.choose){
      store.weddingId=action.choose;sessionStorage.setItem("wedflow_wedding",store.weddingId);
      location.hash="#/planning/ceremonies";await navigate();return;
    }
    const wedding=encodeURIComponent(store.weddingId);
    if(action.task){await api("/weddings/"+wedding+"/tasks/"+encodeURIComponent(action.task)+"/complete",{method:"PATCH"});toast("Task completed.");}
    if(action.cancel){
      if(!confirm("Cancel this booking? Refunds, if applicable, must be requested separately."))return;
      await api("/weddings/"+wedding+"/bookings/"+encodeURIComponent(action.cancel)+"/cancel",{method:"POST"});
      toast("Booking cancelled. Payment/refund status must be checked separately.");
    }
    if(action.checkout){await checkout(action.checkout);return;}
    if(action.read){await api("/notifications/"+encodeURIComponent(action.read)+"/read",{method:"PATCH"});toast("Notification marked read.");}
    if(action.vendorApprove||action.vendorReject){
      const approve=!!action.vendorApprove;const id=action.vendorApprove||action.vendorReject;
      if(!confirm((approve?"Approve":"Reject")+" this vendor profile?"))return;
      await api("/admin/vendors/"+encodeURIComponent(id)+"/"+(approve?"approve":"reject"),{method:"POST"});
      toast("Vendor "+(approve?"approved":"rejected")+".");
    }
    await navigate();
  }catch(err){toast(err.message,true);}finally{if(target.isConnected)target.disabled=false;}
});
root.addEventListener("change",async ev=>{
  const id=ev.target?.getAttribute?.("data-rsvp");
  if(!id)return;
  try{
    await api("/weddings/"+encodeURIComponent(store.weddingId)+"/guests/"+encodeURIComponent(id)+"/rsvp?status="+encodeURIComponent(ev.target.value),{method:"PATCH"});
    toast("RSVP status updated.");
  }catch(err){toast(err.message,true);await navigate();}
});
window.addEventListener("hashchange",navigate);
if(!location.hash)location.hash="#/overview";
navigate();
