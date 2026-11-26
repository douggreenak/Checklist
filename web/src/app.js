import { SchemeVibrant, SchemeTonalSpot, Hct, MaterialDynamicColors as D } from '@material/material-color-utilities';

/* ================= constants ================= */
const PALETTE = [0xFF6C4DD6, 0xFF1E7BE0, 0xFF00897B, 0xFF3FA34D, 0xFFE8710A, 0xFFD81B60,
  0xFFE53935, 0xFF8E24AA, 0xFF00ACC1, 0xFF7CB342, 0xFFFDD835, 0xFF546E7A].map(n => n | 0);

const ICONS = {
  check: 'M9 16.17 4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z',
  close: 'M19 6.41 17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z',
  add: 'M19 13h-6v6h-2v-6H5v-2h6V5h2v6h6z',
  back: 'M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z',
  more: 'M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z',
  settings: 'M19.14 12.94c.04-.3.06-.61.06-.94 0-.32-.02-.64-.07-.94l2.03-1.58c.18-.14.23-.41.12-.61l-1.92-3.32c-.12-.22-.37-.29-.59-.22l-2.39.96c-.5-.38-1.03-.7-1.62-.94l-.36-2.54c-.04-.24-.24-.41-.48-.41h-3.84c-.24 0-.43.17-.47.41l-.36 2.54c-.59.24-1.13.57-1.62.94l-2.39-.96c-.22-.08-.47 0-.59.22L2.74 8.87c-.12.21-.08.47.12.61l2.03 1.58c-.05.3-.09.63-.09.94s.02.64.07.94l-2.03 1.58c-.18.14-.23.41-.12.61l1.92 3.32c.12.22.37.29.59.22l2.39-.96c.5.38 1.03.7 1.62.94l.36 2.54c.05.24.24.41.48.41h3.84c.24 0 .44-.17.47-.41l.36-2.54c.59-.24 1.13-.56 1.62-.94l2.39.96c.22.08.47 0 .59-.22l1.92-3.32c.12-.22.07-.47-.12-.61l-2.01-1.58zM12 15.6c-1.98 0-3.6-1.62-3.6-3.6s1.62-3.6 3.6-3.6 3.6 1.62 3.6 3.6-1.62 3.6-3.6 3.6z',
  expand: 'M16.59 8.59 12 13.17 7.41 8.59 6 10l6 6 6-6z',
  trash: 'M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z',
  undo: 'M12 5V1L7 6l5 5V7c3.31 0 6 2.69 6 6s-2.69 6-6 6-6-2.69-6-6H4c0 4.42 3.58 8 8 8s8-3.58 8-8-3.58-8-8-8z',
  lock: 'M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z',
  palette: 'M12 2C6.49 2 2 6.49 2 12s4.49 10 10 10c1.38 0 2.5-1.12 2.5-2.5 0-.61-.23-1.2-.64-1.67-.08-.1-.13-.21-.13-.33 0-.28.22-.5.5-.5H16c3.31 0 6-2.69 6-6 0-4.96-4.49-9-10-9zm-5.5 9c-.83 0-1.5-.67-1.5-1.5S5.67 8 6.5 8 8 8.67 8 9.5 7.33 11 6.5 11zm3-4C8.67 7 8 6.33 8 5.5S8.67 4 9.5 4s1.5.67 1.5 1.5S10.33 7 9.5 7zm5 0c-.83 0-1.5-.67-1.5-1.5S13.67 4 14.5 4s1.5.67 1.5 1.5S15.33 7 14.5 7zm3 4c-.83 0-1.5-.67-1.5-1.5S16.67 8 17.5 8s1.5.67 1.5 1.5-.67 1.5-1.5 1.5z',
};

/* ================= tiny DOM helper (text is always set as text, never as HTML) ================= */
function h(tag, props = {}, ...kids) {
  const el = document.createElement(tag);
  for (const [k, v] of Object.entries(props)) {
    if (v == null || v === false) continue;
    if (k === 'class') el.className = v;
    else if (k === 'text') el.textContent = v;
    else if (k === 'data') Object.assign(el.dataset, v);
    else if (k.startsWith('on')) el.addEventListener(k.slice(2), v);
    else el.setAttribute(k, v === true ? '' : v);
  }
  for (const kid of kids.flat()) if (kid != null && kid !== false) el.append(kid);
  return el;
}
function icon(name) {
  const svg = document.createElementNS('http://www.w3.org/2000/svg', 'svg');
  svg.setAttribute('viewBox', '0 0 24 24');
  svg.setAttribute('aria-hidden', 'true');
  const p = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  p.setAttribute('d', ICONS[name]);
  svg.append(p);
  return svg;
}

/* ================= state & storage ================= */
const LS = {
  get(k, d) { try { const v = localStorage.getItem(k); return v == null ? d : JSON.parse(v); } catch { return d; } },
  set(k, v) { localStorage.setItem(k, JSON.stringify(v)); },
  del(k) { localStorage.removeItem(k); },
};

let nextUid = 1;
const uid = () => nextUid++;

const state = {
  token: LS.get('cl.token', null),
  settings: { mode: 0, color: PALETTE[0], black: false, ...LS.get('cl.settings', {}) },
  lists: [],
  focusId: null,
  status: '',
  pwOpen: false,
};

function parseLists(json) {
  return JSON.parse(json).map(l => ({
    id: String(l.id),
    name: l.name ?? '',
    showCompleted: !!l.showCompleted,
    argb: Number.isInteger(l.argb) ? l.argb : PALETTE[0],
    items: (l.items || []).map(i => ({ uid: uid(), t: String(i.t ?? ''), c: !!i.c })),
  }));
}
function serialize() {
  return JSON.stringify(state.lists.map(l => ({
    id: l.id, name: l.name, showCompleted: l.showCompleted, argb: l.argb,
    items: l.items.map(i => ({ t: i.t, c: i.c })),
  })));
}
function loadCache() {
  try { state.lists = parseLists(LS.get('cl.lists', '[]')); } catch { state.lists = []; }
}
const remaining = l => l.items.filter(i => !i.c).length;
const done = l => l.items.filter(i => i.c).length;

/* ================= theme (Material 3 "Vibrant" scheme from a seed color) ================= */
const ROLE = {
  primary: D.primary, onPrimary: D.onPrimary, primaryContainer: D.primaryContainer, onPrimaryContainer: D.onPrimaryContainer,
  secondaryContainer: D.secondaryContainer, onSecondaryContainer: D.onSecondaryContainer,
  tertiaryContainer: D.tertiaryContainer, onTertiaryContainer: D.onTertiaryContainer,
  surface: D.surface, surfaceContainerLow: D.surfaceContainerLow, surfaceContainer: D.surfaceContainer,
  surfaceContainerHigh: D.surfaceContainerHigh, surfaceContainerHighest: D.surfaceContainerHighest,
  onSurface: D.onSurface, onSurfaceVariant: D.onSurfaceVariant, outline: D.outline, outlineVariant: D.outlineVariant,
  error: D.error, onError: D.onError, errorContainer: D.errorContainer, onErrorContainer: D.onErrorContainer,
};
const kebab = s => s.replace(/[A-Z]/g, c => '-' + c.toLowerCase());
const hex = n => '#' + (n & 0xFFFFFF).toString(16).padStart(6, '0');
const varCache = new Map();

function schemeVars(argb, dark, black) {
  const key = `${argb}|${dark}|${black}`;
  if (varCache.has(key)) return varCache.get(key);
  const Scheme = dark ? SchemeTonalSpot : SchemeVibrant; // vivid in light mode, softer in dark
  const scheme = new Scheme(Hct.fromInt((argb | 0xFF000000) >>> 0), dark, 0);
  const v = {};
  for (const [name, role] of Object.entries(ROLE)) v['--' + kebab(name)] = hex(role.getArgb(scheme));
  if (dark && black) Object.assign(v, {
    '--surface': '#000000', '--surface-container-low': '#0b0b0b', '--surface-container': '#141414',
    '--surface-container-high': '#1c1c1c', '--surface-container-highest': '#262626',
  });
  varCache.set(key, v);
  return v;
}
const applyVars = (el, vars) => { for (const [k, val] of Object.entries(vars)) el.style.setProperty(k, val); };

const darkQuery = window.matchMedia('(prefers-color-scheme: dark)');
const isDark = () => state.settings.mode === 2 || (state.settings.mode === 0 && darkQuery.matches);
darkQuery.addEventListener('change', () => render());

function applyTheme() {
  const dark = isDark();
  const vars = schemeVars(state.settings.color, dark, state.settings.black);
  applyVars(document.documentElement, vars);
  document.documentElement.style.colorScheme = dark ? 'dark' : 'light';
  document.querySelector('meta[name=theme-color]').setAttribute('content', vars['--surface']);
}
const listVars = l => schemeVars(l.argb, isDark(), state.settings.black);

/* ================= server sync (same API and data format as the Android app) ================= */
class ApiError extends Error { constructor(code, msg) { super(msg); this.code = code; } }

async function api(action, body, token) {
  let res;
  try {
    res = await fetch('api.php?action=' + action, {
      method: body ? 'POST' : 'GET',
      headers: { ...(token ? { Authorization: 'Bearer ' + token } : {}), ...(body ? { 'Content-Type': 'application/json' } : {}) },
      body: body ? JSON.stringify(body) : undefined,
      cache: 'no-store',
    });
  } catch { throw new ApiError(0, "Can't reach the server. Check your connection."); }
  let data = null;
  try { data = await res.json(); } catch { /* not json */ }
  if (!res.ok) throw new ApiError(res.status, data?.error || `Server error (${res.status})`);
  return data;
}

let synced = null;        // what the server is known to hold (serialized)
let pushTimer = null, pollTimer = null, syncRunning = false;

function saveLocal() { LS.set('cl.lists', serialize()); }
function edited() { saveLocal(); schedulePush(); }
function changed() { edited(); render(); }

function setStatus(s) {
  state.status = s;
  const el = document.querySelector('[data-role=status]');
  if (el) el.textContent = s;
}

function authFailed(e) {
  if (e instanceof ApiError && e.code === 401) { signOut(); return true; }
  return false;
}

async function pull() {
  const r = await api('get', null, state.token);
  if (r.json == null) {
    const json = serialize();
    await api('save', { json }, state.token);
    synced = json;
  } else if (synced === null || serialize() === synced) {
    const before = serialize();
    state.lists = parseLists(r.json);
    synced = serialize();
    if (synced !== before) { saveLocal(); render(); }
  }
}

async function push() {
  if (synced === null) return;               // never overwrite the server before a first successful pull
  const json = serialize();
  if (json === synced) return;
  await api('save', { json }, state.token);
  synced = json;
}

function schedulePush() {
  clearTimeout(pushTimer);
  pushTimer = setTimeout(async () => {
    if (!state.token) return;
    try { await push(); setStatus('Synced'); }
    catch (e) { if (!authFailed(e)) setStatus(e.code === 0 ? 'Offline – will retry' : e.message); }
  }, 800);
}

async function syncOnce() {
  if (!state.token || syncRunning) return;
  syncRunning = true;
  try {
    if (synced === null || serialize() === synced) await pull(); else await push();
    setStatus('Synced');
  } catch (e) { if (!authFailed(e)) setStatus(e.code === 0 ? 'Offline – will retry' : e.message); }
  finally { syncRunning = false; }
}

function startSync() {
  stopSync();
  synced = null;
  setStatus('Syncing…');
  syncOnce();
  pollTimer = setInterval(syncOnce, 20000);
}
function stopSync() { clearInterval(pollTimer); clearTimeout(pushTimer); pollTimer = null; }
document.addEventListener('visibilitychange', () => { if (!document.hidden) syncOnce(); });

function signOut() {
  stopSync();
  state.token = null; state.lists = []; synced = null; state.pwOpen = false;
  LS.del('cl.token'); LS.del('cl.lists');
  location.hash = '';
  render();
}

/* ================= routing ================= */
function route() {
  const m = location.hash.match(/^#\/list\/(.+)$/);
  if (m) { const l = state.lists.find(x => x.id === decodeURIComponent(m[1])); if (l) return { name: 'list', list: l }; }
  if (location.hash === '#/settings') return { name: 'settings' };
  return { name: 'home' };
}
const go = hash => { location.hash = hash; };
window.addEventListener('hashchange', () => render());

/* ================= rendering ================= */
function captureView() {
  const a = document.activeElement;
  const pane = document.querySelector('.main-pane'), panel = document.querySelector('.lists-panel');
  return {
    focus: a && a.dataset && (a.dataset.item || a.dataset.role) ? { item: a.dataset.item, role: a.dataset.role, s: a.selectionStart, e: a.selectionEnd } : null,
    paneTop: pane ? pane.scrollTop : 0, panelTop: panel ? panel.scrollTop : 0,
  };
}
function restoreView(v) {
  const pane = document.querySelector('.main-pane'), panel = document.querySelector('.lists-panel');
  if (pane) pane.scrollTop = v.paneTop;
  if (panel) panel.scrollTop = v.panelTop;
  let target = null;
  if (state.focusId != null) { target = document.querySelector(`[data-item="${state.focusId}"]`); state.focusId = null; if (target) target.scrollIntoView({ block: 'center' }); }
  else if (v.focus) target = document.querySelector(v.focus.item ? `[data-item="${v.focus.item}"]` : `[data-role="${v.focus.role}"]`);
  if (target) { target.focus({ preventScroll: true }); if (v.focus && target.setSelectionRange && v.focus.s != null && state.focusId == null) { try { target.setSelectionRange(v.focus.s, v.focus.e); } catch { /* ignore */ } } }
  document.querySelectorAll('.task textarea').forEach(autosize);
}

function render() {
  const view = captureView();
  applyTheme();
  const root = document.getElementById('app');
  root.replaceChildren(state.token ? shell() : loginView());
  restoreView(view);
}

function autosize(t) { t.style.height = 'auto'; t.style.height = t.scrollHeight + 'px'; }
const today = () => new Date().toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric' });

function progressBar(l) {
  const total = l.items.length;
  return h('div', { class: 'progress', role: 'progressbar', 'aria-valuemin': 0, 'aria-valuemax': total, 'aria-valuenow': done(l) },
    (() => { const i = document.createElement('i'); i.style.width = total ? (done(l) / total * 100) + '%' : '0%'; return i; })());
}

/* ----- swipe (touch / pen) ----- */
function swipeable(card, { right, left, rightLabel, rightIcon }) {
  const wrap = h('div', { class: 'swipe' },
    h('div', { class: 'swipe-bg' },
      h('div', { class: 'l' }, icon(rightIcon || 'check'), rightLabel || ''),
      h('div', { class: 'r' }, 'Delete', icon('trash'))),
    card);
  card.classList.add('card');
  let sx = 0, sy = 0, dx = 0, active = false, tracking = false, pid = null;
  const reset = () => { wrap.classList.add('snap'); card.style.transform = ''; wrap.classList.remove('dir-left', 'dir-right'); };
  card.addEventListener('pointerdown', e => {
    if (e.pointerType === 'mouse' || e.target.closest('button')) return;
    sx = e.clientX; sy = e.clientY; dx = 0; tracking = true; active = false; pid = e.pointerId; wrap.classList.remove('snap');
  });
  card.addEventListener('pointermove', e => {
    if (!tracking || e.pointerId !== pid) return;
    const mx = e.clientX - sx, my = e.clientY - sy;
    if (!active) {
      if (Math.abs(mx) > 12 && Math.abs(mx) > Math.abs(my) * 1.5) { active = true; try { card.setPointerCapture(pid); } catch { /* ignore */ } }
      else if (Math.abs(my) > 12) { tracking = false; return; }
      else return;
    }
    dx = (!right && mx > 0) ? 0 : mx;
    card.style.transform = `translateX(${dx}px)`;
    wrap.classList.toggle('dir-right', dx > 8);
    wrap.classList.toggle('dir-left', dx < -8);
  });
  const end = () => {
    if (!tracking) return;
    tracking = false;
    const w = wrap.offsetWidth, limit = Math.min(120, w * 0.3);
    if (active && dx > limit && right) { reset(); right(); }
    else if (active && dx < -limit && left) { wrap.classList.add('leaving'); setTimeout(left, 240); }
    else reset();
    active = false;
  };
  card.addEventListener('pointerup', end);
  card.addEventListener('pointercancel', end);
  return wrap;
}

/* ----- list panel (home on phones, sidebar on desktop) ----- */
function listCard(l, current) {
  const card = h('button', { class: 'list-card' + (current ? ' current' : ''), type: 'button', onclick: () => go('#/list/' + encodeURIComponent(l.id)) },
    h('div', { class: 'row' },
      h('div', { class: 'avatar', text: (l.name.trim()[0] || '•').toUpperCase() }),
      h('div', { class: 'meta' },
        h('div', { class: 'name', text: l.name.trim() || 'Untitled' }),
        h('div', { class: 'sub', text: !l.items.length ? 'Empty' : remaining(l) === 0 ? 'All done' : `${remaining(l)} to do` + (done(l) ? ` · ${done(l)} done` : '') })),
      remaining(l) > 0 ? h('div', { class: 'count', text: String(remaining(l)) }) : l.items.length ? h('div', { class: 'count' }, icon('check')) : null),
    l.items.length ? progressBar(l) : null);
  applyVars(card, listVars(l));
  const wrap = swipeable(card, { left: () => { state.lists = state.lists.filter(x => x !== l); if (route().name === 'list' && route().list === l) go(''); changed(); } });
  wrap.querySelector('.swipe-bg .l').style.display = 'none';
  return wrap;
}

function listsPanel(r) {
  const todo = state.lists.reduce((n, l) => n + remaining(l), 0);
  return h('aside', { class: 'lists-panel' },
    h('div', { class: 'pane-inner' },
      h('div', { class: 'top-row' }, h('span', { class: 'grow' }), h('button', { class: 'icon-btn', 'aria-label': 'Settings', onclick: () => go('#/settings') }, icon('settings'))),
      h('div', { class: 'panel-head' }, h('div', {}, h('h1', { class: 'display', text: 'My Lists' }), h('p', { class: 'label', text: `${today()} · ${todo} to do` }))),
      state.lists.length
        ? h('div', { class: 'stack' }, state.lists.map(l => listCard(l, r.list === l)))
        : h('div', { class: 'empty' }, h('div', { class: 'big', text: '📋' }), h('h3', { text: 'No lists yet' }), h('p', { text: 'Tap “New list” to make your first one.' }))),
    h('div', { class: 'fab-wrap' }, h('button', { class: 'fab', type: 'button', onclick: newList }, icon('add'), 'New list')));
}

function newList() {
  const l = { id: crypto.randomUUID(), name: 'New list', showCompleted: false, argb: PALETTE[state.lists.length % PALETTE.length], items: [] };
  state.lists.push(l);
  edited();
  go('#/list/' + encodeURIComponent(l.id));
}

/* ----- a single list ----- */
function addTaskAfter(l, item) {
  const n = { uid: uid(), t: '', c: false };
  const at = item ? l.items.indexOf(item) + 1 : l.items.length;
  l.items.splice(at, 0, n);
  state.focusId = n.uid;
  changed();
}

function taskRow(l, item) {
  const ta = h('textarea', { rows: 1, placeholder: 'Type task…', enterkeyhint: 'next', 'aria-label': 'Task', data: { item: item.uid },
    oninput: e => {
      const t = e.target;
      if (t.value.includes('\n')) { item.t = t.value.replace(/\n/g, ''); t.value = item.t; addTaskAfter(l, item); return; }
      item.t = t.value; autosize(t); edited();
    },
    onkeydown: e => { if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) { e.preventDefault(); addTaskAfter(l, item); } } });
  ta.value = item.t;
  const toggle = () => { item.c = !item.c; changed(); };
  const remove = () => { l.items.splice(l.items.indexOf(item), 1); changed(); };
  const card = h('div', { class: 'task' + (item.c ? ' done' : '') },
    h('button', { class: 'check', type: 'button', role: 'checkbox', 'aria-checked': String(item.c), 'aria-label': item.c ? 'Mark not done' : 'Mark done', onclick: toggle },
      h('span', { class: 'box' }, icon('check'))),
    ta,
    h('button', { class: 'del', type: 'button', 'aria-label': 'Delete task', onclick: remove }, icon('close')));
  return swipeable(card, { right: toggle, left: remove, rightLabel: item.c ? 'Not done' : 'Done', rightIcon: item.c ? 'undo' : 'check' });
}

function openMenu(btn, l) {
  document.querySelector('.menu')?.remove();
  const close = () => { menu.remove(); document.removeEventListener('pointerdown', outside, true); };
  const outside = e => { if (!menu.contains(e.target) && !btn.contains(e.target)) close(); };
  const item = (ic, text, fn, cls) => h('button', { type: 'button', class: cls || '', onclick: () => { close(); fn(); } }, ic, text);
  const menu = h('div', { class: 'menu', role: 'menu' },
    item(icon('check'), l.showCompleted ? 'Hide completed' : 'Show completed', () => { l.showCompleted = !l.showCompleted; changed(); }),
    item(h('span', { class: 'dot' }), 'Change color', () => colorDialog(l)),
    item(icon('trash'), 'Delete list', () => { state.lists = state.lists.filter(x => x !== l); go(''); changed(); }, 'danger'));
  btn.parentElement.append(menu);
  document.addEventListener('pointerdown', outside, true);
}

function listView(l) {
  const active = l.items.filter(i => !i.c), completed = l.items.filter(i => i.c);
  const more = h('button', { class: 'icon-btn', type: 'button', 'aria-label': 'List menu', onclick: e => openMenu(e.currentTarget, l) }, icon('more'));
  const title = h('input', { class: 'title-input', 'aria-label': 'List name', data: { role: 'title' }, maxlength: 80,
    oninput: e => { l.name = e.target.value; edited(); } });
  title.value = l.name;

  const view = h('div', { class: 'pane-inner' },
    h('div', { class: 'top-row' },
      h('button', { class: 'icon-btn back-btn', type: 'button', 'aria-label': 'Back', onclick: () => go('') }, icon('back')),
      h('span', { class: 'grow' }),
      h('div', { class: 'menu-wrap' }, more)),
    title,
    h('p', { class: 'label', text: l.items.length ? `${remaining(l)} to do · ${done(l)} done` : 'No tasks' }),
    l.items.length ? h('div', { class: 'mt14' }, progressBar(l)) : null,
    h('div', { class: 'tasks' },
      !active.length ? (completed.length
        ? h('div', { class: 'empty' }, h('div', { class: 'big', text: '🎉' }), h('h3', { text: 'All done!' }), h('p', { text: 'Nothing left to do on this list.' }))
        : h('div', { class: 'empty' }, h('div', { class: 'big', text: '📝' }), h('h3', { text: 'No tasks yet' }), h('p', { text: 'Tap “Add task”, type, and press Enter for the next one.' }))) : null,
      active.map(i => taskRow(l, i)),
      completed.length ? h('button', { class: 'completed-head', type: 'button', 'aria-expanded': String(l.showCompleted), onclick: () => { l.showCompleted = !l.showCompleted; changed(); } },
        icon('expand'), 'Completed', h('span', { class: 'badge', text: String(completed.length) })) : null,
      l.showCompleted ? completed.map(i => taskRow(l, i)) : null));

  const root = h('section', { class: 'list-view' }, view,
    h('div', { class: 'fab-wrap' }, h('button', { class: 'fab', type: 'button', onclick: () => addTaskAfter(l, active[active.length - 1]) }, icon('add'), 'Add task')));
  applyVars(root, listVars(l));
  root.style.background = 'var(--surface)';
  return root;
}

/* ----- color picker (shared by list dialog and settings) ----- */
function hueOf(argb) {
  const r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
  const mx = Math.max(r, g, b), mn = Math.min(r, g, b), d = mx - mn;
  if (!d) return 0;
  let hh = mx === r ? ((g - b) / d) % 6 : mx === g ? (b - r) / d + 2 : (r - g) / d + 4;
  return Math.round(((hh * 60) + 360) % 360);
}
function argbFromHue(hue) {
  const s = 0.75, v = 0.85, c = v * s, x = c * (1 - Math.abs((hue / 60) % 2 - 1)), m = v - c;
  const [r, g, b] = hue < 60 ? [c, x, 0] : hue < 120 ? [x, c, 0] : hue < 180 ? [0, c, x] : hue < 240 ? [0, x, c] : hue < 300 ? [x, 0, c] : [c, 0, x];
  return ((0xFF << 24) | (Math.round((r + m) * 255) << 16) | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255)) | 0;
}
function colorPicker(getColor, setColor) {
  const box = h('div', {});
  const swatches = h('div', { class: 'swatches' });
  const slider = h('input', { class: 'hue', type: 'range', min: 0, max: 360, 'aria-label': 'Custom color hue',
    oninput: e => { setColor(argbFromHue(+e.target.value)); paint(false); } });
  function paint(updateSlider = true) {
    const cur = getColor();
    swatches.replaceChildren(...PALETTE.map(c => {
      const b = h('button', { class: 'swatch', type: 'button', 'aria-label': 'Choose color', 'aria-pressed': String(c === cur), onclick: () => { setColor(c); paint(); } }, icon('check'));
      b.style.background = hex(c);
      b.style.color = (((c >> 16) & 255) * 0.299 + ((c >> 8) & 255) * 0.587 + (c & 255) * 0.114) > 160 ? '#000' : '#fff';
      return b;
    }));
    if (updateSlider) slider.value = hueOf(cur);
  }
  box.append(swatches, h('p', { class: 'label hue-label', text: 'Custom color' }), slider);
  paint();
  return box;
}

function colorDialog(l) {
  const dlg = h('dialog', {},
    h('h2', { text: 'List color' }),
    colorPicker(() => l.argb, c => { l.argb = c; saveLocal(); schedulePush(); applyVars(document.querySelector('.list-view'), listVars(l)); applyVars(dlg, listVars(l)); }),
    h('div', { class: 'actions' }, h('button', { class: 'filled-btn', type: 'button', text: 'Done', onclick: () => dlg.close() })));
  applyVars(dlg, listVars(l));
  dlg.addEventListener('close', () => { dlg.remove(); render(); });
  document.body.append(dlg);
  dlg.showModal();
}

/* ----- settings ----- */
function saveSettings() { LS.set('cl.settings', state.settings); }

function settingsView() {
  const s = state.settings;
  const modeBtn = (i, label) => h('button', { type: 'button', 'aria-pressed': String(s.mode === i), text: label, onclick: () => { s.mode = i; saveSettings(); render(); } });

  const pw = { cur: '', nw: '', cf: '' };
  const msg = h('p', { class: 'msg hidden' });
  const field = (label, key, auto) => h('label', { class: 'field' }, h('span', { text: label }),
    h('input', { type: 'password', autocomplete: auto, oninput: e => { pw[key] = e.target.value; } }));
  const saveBtn = h('button', { class: 'filled-btn', type: 'button', text: 'Save new password' });
  const show = (text, ok) => { msg.textContent = text; msg.className = 'msg ' + (ok ? 'ok' : 'err'); };
  saveBtn.addEventListener('click', async () => {
    if (pw.nw.length < 6) return show('The new password must be at least 6 characters', false);
    if (pw.nw !== pw.cf) return show("The new passwords don't match", false);
    saveBtn.disabled = true; saveBtn.textContent = 'Saving…';
    try {
      const r = await api('changepw', { current: pw.cur, new: pw.nw }, state.token);
      state.token = r.token; LS.set('cl.token', r.token);
      show('Password changed. Other devices were signed out.', true);
      form.querySelectorAll('input').forEach(i => { i.value = ''; }); pw.cur = pw.nw = pw.cf = '';
    } catch (e) { show(e.message, false); }
    saveBtn.disabled = false; saveBtn.textContent = 'Save new password';
  });
  const form = h('div', { class: 'stack-s' + (state.pwOpen ? '' : ' hidden') },
    field('Current password', 'cur', 'current-password'), field('New password', 'nw', 'new-password'), field('Repeat new password', 'cf', 'new-password'), msg, saveBtn);

  return h('div', { class: 'pane-inner' },
    h('div', { class: 'top-row' },
      h('button', { class: 'icon-btn back-btn', type: 'button', 'aria-label': 'Back', onclick: () => go('') }, icon('back')), h('span', { class: 'grow' })),
    h('h1', { class: 'display', text: 'Settings' }),

    h('h2', { class: 'section-title', text: 'Appearance' }),
    h('div', { class: 'seg', role: 'group', 'aria-label': 'Appearance' }, modeBtn(0, 'System'), modeBtn(1, 'Light'), modeBtn(2, 'Dark')),

    h('h2', { class: 'section-title', text: 'Theme color' }),
    h('div', { class: 'card' }, h('p', { text: 'Tints backgrounds, buttons and cards across the whole app. Each list keeps its own color.' }),
      colorPicker(() => s.color, c => { s.color = c; saveSettings(); applyTheme(); })),

    h('h2', { class: 'section-title', text: 'Dark mode' }),
    h('div', { class: 'card' }, h('div', { class: 'switch-row' },
      h('div', { class: 't' }, 'Pure black background', h('small', { text: 'Saves battery on OLED screens' })),
      h('button', { class: 'switch', type: 'button', role: 'switch', 'aria-checked': String(s.black), 'aria-label': 'Pure black background', onclick: () => { s.black = !s.black; saveSettings(); render(); } }))),

    h('h2', { class: 'section-title', text: 'Account' }),
    h('div', { class: 'card stack-s' },
      h('div', { class: 'account', text: location.host }),
      h('div', { class: 'status', 'data-role': 'status', text: state.status }),
      h('button', { class: 'tonal-btn', type: 'button', text: 'Change password', onclick: () => { state.pwOpen = !state.pwOpen; form.classList.toggle('hidden', !state.pwOpen); } }),
      form,
      h('button', { class: 'tonal-btn danger', type: 'button', text: 'Sign out', onclick: signOut })),

    h('div', { class: 'mt24' },
      h('button', { class: 'tonal-btn', type: 'button', text: 'Reset appearance', onclick: () => { Object.assign(s, { mode: 0, color: PALETTE[0], black: false }); saveSettings(); render(); } })));
}

/* ----- shell & login ----- */
function shell() {
  const r = route();
  const main = h('main', { class: 'main-pane' },
    r.name === 'list' ? listView(r.list)
      : r.name === 'settings' ? settingsView()
        : h('div', { class: 'pane-inner' }, h('div', { class: 'empty' }, h('div', { class: 'big', text: '✅' }), h('h3', { text: 'Pick a list' }), h('p', { text: 'Choose one on the left, or make a new one.' }))));
  return h('div', { class: 'shell view-' + r.name }, listsPanel(r), main);
}

function loginView() {
  const err = h('p', { class: 'msg err hidden', role: 'alert' });
  const pass = h('input', { type: 'password', autocomplete: 'current-password', 'aria-label': 'Password', required: true, autofocus: true });
  const btn = h('button', { class: 'filled-btn', type: 'submit', text: 'Sign in' });
  const form = h('form', { onsubmit: async e => {
    e.preventDefault();
    btn.disabled = true; btn.textContent = 'Signing in…'; err.classList.add('hidden');
    try {
      const r = await api('login', { password: pass.value });
      state.token = r.token; LS.set('cl.token', r.token);
      loadCache(); render(); startSync();
    } catch (x) { err.textContent = x.message; err.classList.remove('hidden'); btn.disabled = false; btn.textContent = 'Sign in'; }
  } },
  h('div', { class: 'lock' }, icon('lock')),
  h('h1', { class: 'display', text: 'Checklist' }),
  h('p', { class: 'label', text: 'Sign in to see your lists' }),
  h('label', { class: 'field' }, h('span', { text: 'Password' }), pass), err, btn);
  return h('div', { class: 'login' }, form);
}

/* ================= boot ================= */
if (state.token) loadCache();
render();
if (state.token) startSync();
