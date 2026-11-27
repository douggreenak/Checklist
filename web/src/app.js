import { ICONS } from './icons.js';
import { SchemeVibrant, SchemeTonalSpot, Hct, MaterialDynamicColors as D } from '@material/material-color-utilities';

/* ================= constants ================= */
const PALETTE = [0xFF6C4DD6, 0xFF1E7BE0, 0xFF00897B, 0xFF3FA34D, 0xFFE8710A, 0xFFD81B60,
  0xFFE53935, 0xFF8E24AA, 0xFF00ACC1, 0xFF7CB342, 0xFFFDD835, 0xFF546E7A].map(n => n | 0);

const ALIAS = { back: 'arrow_back', more: 'more_vert', expand: 'keyboard_arrow_down', trash: 'delete' };

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
  svg.setAttribute('viewBox', '0 -960 960 960');
  svg.setAttribute('aria-hidden', 'true');
  const p = document.createElementNS('http://www.w3.org/2000/svg', 'path');
  p.setAttribute('d', ICONS[ALIAS[name] || name]);
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
  showAllCompleted: false,
  status: '',
  pwOpen: false,
};

const newId = () => (crypto.randomUUID ? crypto.randomUUID() : String(Date.now()) + Math.random().toString(16).slice(2));
function parseLists(json) {
  return JSON.parse(json).map(l => ({
    id: String(l.id),
    name: l.name ?? '',
    showCompleted: !!l.showCompleted,
    argb: Number.isInteger(l.argb) ? l.argb : PALETTE[0],
    items: (l.items || []).map(i => ({ uid: uid(), i: i.i ? String(i.i) : newId(), t: String(i.t ?? ''), c: !!i.c })),
  }));
}
function serialize() {
  return JSON.stringify(state.lists.map(l => ({
    id: l.id, name: l.name, showCompleted: l.showCompleted, argb: l.argb,
    items: l.items.map(i => ({ i: i.i, t: i.t, c: i.c })),
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

/*
 * Live sync. Each change is sent to the server within a moment; the server merges it with what other people did
 * (field by field, newest change wins) and sends back the combined list. We also ask the server every couple of
 * seconds whether anything changed, so two people see each other's check-offs almost immediately.
 */
let prevDoc = LS.get('cl.doc', null);   // the last document the server confirmed (with change times); null until the first sync
let knownRev = null;
let pushTimer = null, pollTimer = null, syncRunning = false, syncAgain = false;

const signature = () => serialize();
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

/** Turns what is on screen into a document, stamping only the fields that changed since [prevDoc] with the current time. */
function buildDoc() {
  const now = Date.now();
  const prev = new Map((prevDoc || []).map(l => [l.id, l]));
  const doc = [];
  for (const l of state.lists) {
    const p = prev.get(l.id);
    const pItems = new Map((p?.items || []).map(i => [i.i, i]));
    const del = { ...(p?.del || {}) };
    const cand = l.items.map(it => { it.i = it.i || newId(); return { it, p: pItems.get(it.i) }; });
    for (const id of pItems.keys()) if (!l.items.some(it => it.i === id)) del[id] = del[id] || now;
    // The longest run of tasks still in order keeps its position keys; moved or new tasks get one between their neighbours.
    const po = cand.map(c => (c.p ? c.p.o : null));
    const best = po.map(() => 0), from = po.map(() => -1);
    let end = -1;
    for (let x = 0; x < po.length; x++) {
      if (po[x] === null) continue;
      best[x] = 1;
      for (let y = 0; y < x; y++) if (po[y] !== null && po[y] < po[x] && best[y] + 1 > best[x]) { best[x] = best[y] + 1; from[x] = y; }
      if (end < 0 || best[x] > best[end]) end = x;
    }
    const os = po.map(() => null);
    for (let c = end; c >= 0; c = from[c]) os[c] = po[c];
    for (let k = 0; k < os.length; k++) {
      if (os[k] !== null) continue;
      let lo = -Infinity, hi = Infinity, from = k;
      for (let j = k - 1; j >= 0; j--) if (os[j] !== null) { lo = os[j]; break; }
      let to = k;
      while (to < os.length && os[to] === null) to++;
      if (to < os.length) hi = os[to];
      const run = to - from;
      for (let j = 0; j < run; j++) {
        os[from + j] = lo === -Infinity && hi === Infinity ? from + j : lo === -Infinity ? hi - (run - j) : hi === Infinity ? lo + j + 1 : lo + (hi - lo) * (j + 1) / (run + 1);
      }
      k = to - 1;
    }
    doc.push({
      id: l.id, name: l.name, showCompleted: l.showCompleted, argb: l.argb, dead: 0, del,
      lu: p && p.name === l.name && p.showCompleted === l.showCompleted && p.argb === l.argb ? p.lu : now,
      items: cand.map((c, k) => ({
        i: c.it.i,
        t: c.it.t, tu: c.p && c.p.t === c.it.t ? c.p.tu : now,
        c: c.it.c, cu: c.p && c.p.c === c.it.c ? c.p.cu : now,
        o: os[k], ou: c.p && c.p.o === os[k] ? c.p.ou : now,
      })),
    });
  }
  for (const p of prev.values()) {
    if (!state.lists.some(l => l.id === p.id)) doc.push(p.dead ? p : { id: p.id, name: '', showCompleted: false, argb: 0, lu: 0, dead: now, del: {}, items: [] });
  }
  return doc;
}

/** Shows a server document on screen. Existing list and task objects are updated in place, because the page's handlers hold them. */
function applyDoc(doc) {
  const oldLists = new Map(state.lists.map(l => [l.id, l]));
  state.lists = doc.filter(l => !l.dead).map(d => {
    const l = oldLists.get(String(d.id)) || { id: String(d.id), items: [] };
    l.name = d.name ?? ''; l.showCompleted = !!d.showCompleted;
    l.argb = Number.isInteger(d.argb) && d.argb !== 0 ? d.argb : PALETTE[0];
    const oldItems = new Map(l.items.map(i => [i.i, i]));
    const items = (d.items || []).map(x => {
      const it = oldItems.get(x.i) || { uid: uid(), i: x.i };
      it.t = String(x.t ?? ''); it.c = !!x.c;
      return it;
    });
    l.items.splice(0, l.items.length, ...items);
    return l;
  });
}

async function syncNow() {
  if (!state.token) return;
  if (syncRunning) { syncAgain = true; return; }
  syncRunning = true;
  try {
    if (prevDoc === null) {
      // First time on this device: take what the server has (upgrading old data), never overwrite it.
      const r = await api('get', null, state.token);
      if (r.doc) {
        const before = signature();
        applyDoc(r.doc); prevDoc = r.doc; knownRev = r.rev; LS.set('cl.doc', prevDoc);
        if (signature() !== before) { saveLocal(); render(); }
      } else prevDoc = [];
    }
    if (prevDoc !== null) {
      const sent = buildDoc(), sig = signature();
      const r = await api('sync', { doc: sent }, state.token);
      if (signature() === sig) {
        const before = sig;
        applyDoc(r.doc); prevDoc = r.doc;
        if (signature() !== before) { saveLocal(); render(); }
      } else { prevDoc = sent; syncAgain = true; }   // edited while waiting: the next round sends those too
      knownRev = r.rev; LS.set('cl.doc', prevDoc);
    }
    setStatus('Synced');
  } catch (e) { if (!authFailed(e)) setStatus(e.code === 0 ? 'Offline – will retry' : e.message); }
  finally {
    syncRunning = false;
    if (syncAgain) { syncAgain = false; schedulePush(50); }
  }
}

function schedulePush(delay = 350) {
  clearTimeout(pushTimer);
  pushTimer = setTimeout(syncNow, delay);
}

async function poll() {
  if (!state.token || syncRunning || document.hidden || prevDoc === null) return;
  try {
    const r = await api('rev', null, state.token);
    if (r.rev !== knownRev) syncNow();
  } catch (e) { if (!authFailed(e)) setStatus(e.code === 0 ? 'Offline – will retry' : e.message); }
}

function startSync() {
  stopSync();
  setStatus('Syncing…');
  syncNow();
  pollTimer = setInterval(poll, 2000);
}
function stopSync() { clearInterval(pollTimer); clearTimeout(pushTimer); pollTimer = null; }
document.addEventListener('visibilitychange', () => { if (!document.hidden) syncNow(); });

function signOut() {
  stopSync();
  state.token = null; state.lists = []; prevDoc = null; knownRev = null; state.pwOpen = false;
  LS.del('cl.token'); LS.del('cl.lists'); LS.del('cl.doc');
  location.hash = '';
  render();
}

/* ================= routing ================= */
function route() {
  const m = location.hash.match(/^#\/list\/(.+)$/);
  if (m) { const l = state.lists.find(x => x.id === decodeURIComponent(m[1])); if (l) return { name: 'list', list: l }; }
  if (location.hash === '#/settings') return { name: 'settings' };
  if (location.hash === '#/all') return { name: 'all' };
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
function swipeable(card, { right, rightLabel, rightIcon }) {
  const wrap = h('div', { class: 'swipe' },
    h('div', { class: 'swipe-bg' },
      h('div', { class: 'l' }, icon(rightIcon || 'check'), rightLabel || '')),
    card);
  card.classList.add('swipe-card');
  let sx = 0, sy = 0, dx = 0, active = false, tracking = false, pid = null;
  const reset = () => { wrap.classList.add('snap'); card.style.transform = ''; wrap.classList.remove('dir-right'); };
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
    dx = Math.max(0, mx);   // swipe right only (done / not done); there is no swipe-to-delete
    card.style.transform = `translateX(${dx}px)`;
    wrap.classList.toggle('dir-right', dx > 8);
  });
  const end = () => {
    if (!tracking) return;
    tracking = false;
    const w = wrap.offsetWidth, limit = w / 3; // must drag a third of the way across
    if (active && dx > limit && right) { reset(); right(); }
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
  return card;
}

function allCard(r) {
  const total = state.lists.reduce((n, l) => n + l.items.length, 0), todo = state.lists.reduce((n, l) => n + remaining(l), 0);
  return h('button', { class: 'list-card all-card' + (r.name === 'all' ? ' current' : ''), type: 'button', onclick: () => go('#/all') },
    h('div', { class: 'row' },
      h('div', { class: 'avatar' }, icon('checklist')),
      h('div', { class: 'meta' },
        h('div', { class: 'name', text: 'All tasks' }),
        h('div', { class: 'sub', text: `${total} in ${state.lists.length} ${state.lists.length === 1 ? 'list' : 'lists'}` })),
      todo > 0 ? h('div', { class: 'count', text: String(todo) }) : null));
}

/* Every task from every list, grouped by list. These are the same item objects the lists hold. */
function openAllMenu(btn, all) {
  document.querySelector('.menu')?.remove();
  const close = () => { menu.remove(); document.removeEventListener('pointerdown', outside, true); };
  const outside = e => { if (!menu.contains(e.target) && !btn.contains(e.target)) close(); };
  const item = (ic, text, fn, off) => h('button', { type: 'button', disabled: off, onclick: () => { close(); fn(); } }, ic, text);
  const menu = h('div', { class: 'menu', role: 'menu' },
    item(icon(state.showAllCompleted ? 'visibility_off' : 'visibility'), state.showAllCompleted ? 'Hide completed' : 'Show completed', () => { state.showAllCompleted = !state.showAllCompleted; render(); }),
    item(icon('undo'), 'Uncheck all', () => { state.lists.forEach(l => l.items.forEach(i => { i.c = false; })); changed(); }, !done(all)));
  btn.parentElement.append(menu);
  document.addEventListener('pointerdown', outside, true);
}

function allView() {
  const all = { items: state.lists.flatMap(l => l.items) };
  const groups = pick => state.lists.map(l => [l, l.items.filter(i => pick(i))]).filter(([, it]) => it.length);
  const group = ([l, items]) => {
    const vars = listVars(l);
    const head = h('div', { class: 'group-head' }, h('span', { class: 'dot' }), l.name.trim() || 'Untitled');
    applyVars(head, vars);
    return [head, ...items.map(i => { const w = taskRow(l, i, true); applyVars(w, vars); return w; })];
  };
  const act = groups(i => !i.c), comp = groups(i => i.c);
  const n = all.items.length, d = done(all);
  const view = h('div', { class: 'pane-inner' },
    h('div', { class: 'top-row' },
      h('button', { class: 'icon-btn back-btn', type: 'button', 'aria-label': 'Back', onclick: () => go('') }, icon('back')),
      h('span', { class: 'grow' }),
      h('span', { class: 'total-pill', 'aria-label': 'Total tasks', text: `${n} ${n === 1 ? 'task' : 'tasks'}` }),
      h('div', { class: 'menu-wrap' }, h('button', { class: 'icon-btn', type: 'button', 'aria-label': 'List menu', onclick: e => openAllMenu(e.currentTarget, all) }, icon('more')))),
    h('h1', { class: 'display all-title', text: 'All tasks' }),
    h('p', { class: 'label', text: n ? `${n - d} to do · ${d} done` : 'No tasks' }),
    n ? h('div', { class: 'mt14' }, progressBar(all)) : null,
    h('div', { class: 'tasks' },
      !act.length ? h('div', { class: 'empty' }, h('div', { class: 'big' }, icon(n ? 'task_alt' : 'edit_note')), h('h3', { text: n ? 'All done!' : 'No tasks yet' }),
        h('p', { text: n ? 'Nothing left to do in any list.' : 'Add tasks inside a list and they all show up here.' })) : act.flatMap(group),
      comp.length ? h('button', { class: 'completed-head', type: 'button', 'aria-expanded': String(state.showAllCompleted), onclick: () => { state.showAllCompleted = !state.showAllCompleted; render(); } },
        icon('expand'), 'Completed', h('span', { class: 'badge', text: String(d) })) : null,
      state.showAllCompleted ? comp.flatMap(group) : null));
  const root = h('section', { class: 'list-view all-view' }, view);
  root.style.background = 'var(--surface)';
  return root;
}

function listsPanel(r) {
  const todo = state.lists.reduce((n, l) => n + remaining(l), 0);
  return h('aside', { class: 'lists-panel' },
    h('div', { class: 'pane-inner' },
      h('div', { class: 'top-row' }, h('span', { class: 'grow' }), h('button', { class: 'icon-btn', 'aria-label': 'Settings', onclick: () => go('#/settings') }, icon('settings'))),
      h('div', { class: 'panel-head' }, h('div', {}, h('h1', { class: 'display', text: 'My Lists' }), h('p', { class: 'label', text: `${today()} · ${todo} to do` }))),
      state.lists.length
        ? h('div', { class: 'stack' }, allCard(r), state.lists.map(l => listCard(l, r.list === l)))
        : h('div', { class: 'empty' }, h('div', { class: 'big' }, icon('checklist')), h('h3', { text: 'No lists yet' }), h('p', { text: 'Tap “New list” to make your first one.' }))),
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

function taskRow(l, item, noDrag = false) {
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
    item.c || noDrag ? null : h('button', { class: 'drag', type: 'button', 'aria-label': 'Drag to reorder', onpointerdown: e => startReorder(e, l, item) }, icon('drag')),
    h('button', { class: 'del', type: 'button', 'aria-label': 'Delete task', onclick: remove }, icon('close')));
  const wrap = swipeable(card, { right: toggle, rightLabel: item.c ? 'Not done' : 'Done', rightIcon: item.c ? 'undo' : 'check' });
  wrap.dataset.uid = item.uid;
  return wrap;
}

/* ----- drag to reorder (active tasks); the new order is saved and synced like any other edit ----- */
function startReorder(e, l, item) {
  if (e.button > 0) return;
  e.preventDefault();
  const handle = e.currentTarget;
  const wraps = [...document.querySelectorAll('.tasks > .swipe[data-uid]')].filter(w => l.items.some(i => String(i.uid) === w.dataset.uid && !i.c));
  const from = wraps.findIndex(w => w.dataset.uid === String(item.uid));
  if (from < 0 || wraps.length < 2) return;
  const scroller = handle.closest('.main-pane, .lists-panel') || document.scrollingElement;
  const rects = wraps.map(w => w.getBoundingClientRect());
  const gap = rects[1].top - rects[0].bottom, size = rects[from].height + gap;
  const startY = e.clientY, startScroll = scroller.scrollTop;
  let clientY = startY, raf = 0, to = from;
  const me = wraps[from];
  me.classList.add('dragging');
  wraps.forEach(w => { if (w !== me) w.classList.add('shifting'); });
  try { handle.setPointerCapture(e.pointerId); } catch { /* ignore */ }
  const update = () => {
    const dy = clientY - startY + (scroller.scrollTop - startScroll);
    me.style.transform = `translateY(${dy}px)`;
    const center = rects[from].top + rects[from].height / 2 + dy;
    to = 0;
    rects.forEach((r, i) => { if (i !== from && r.top + r.height / 2 < center) to++; });
    wraps.forEach((w, i) => {
      if (w === me) return;
      const shift = from < to && i > from && i <= to ? -size : from > to && i >= to && i < from ? size : 0;
      w.style.transform = shift ? `translateY(${shift}px)` : '';
    });
  };
  const edge = () => {
    const b = scroller.getBoundingClientRect(), pad = 70;
    const v = clientY < b.top + pad ? -16 : clientY > b.bottom - pad ? 16 : 0;
    if (v) { scroller.scrollTop += v; update(); }
    raf = requestAnimationFrame(edge);
  };
  raf = requestAnimationFrame(edge);
  const move = ev => { if (ev.pointerId === e.pointerId) { clientY = ev.clientY; update(); } };
  const finish = ev => {
    if (ev.pointerId !== e.pointerId) return;
    cancelAnimationFrame(raf);
    handle.removeEventListener('pointermove', move); handle.removeEventListener('pointerup', finish); handle.removeEventListener('pointercancel', finish);
    wraps.forEach(w => { w.classList.remove('dragging', 'shifting'); w.style.transform = ''; });
    if (ev.type === 'pointerup' && to !== from) {
      const slots = l.items.map((i, k) => i.c ? -1 : k).filter(k => k >= 0);
      const order = slots.map(k => l.items[k]);
      order.splice(to, 0, order.splice(from, 1)[0]);
      slots.forEach((k, n) => { l.items[k] = order[n]; });
      changed();
    }
  };
  handle.addEventListener('pointermove', move); handle.addEventListener('pointerup', finish); handle.addEventListener('pointercancel', finish);
}

function openMenu(btn, l) {
  document.querySelector('.menu')?.remove();
  const close = () => { menu.remove(); document.removeEventListener('pointerdown', outside, true); };
  const outside = e => { if (!menu.contains(e.target) && !btn.contains(e.target)) close(); };
  const item = (ic, text, fn, cls) => h('button', { type: 'button', class: cls || '', disabled: cls === 'disabled', onclick: () => { close(); fn(); } }, ic, text);
  const menu = h('div', { class: 'menu', role: 'menu' },
    item(icon(l.showCompleted ? 'visibility_off' : 'visibility'), l.showCompleted ? 'Hide completed' : 'Show completed', () => { l.showCompleted = !l.showCompleted; changed(); }),
    item(icon('undo'), 'Uncheck all', () => { l.items.forEach(i => { i.c = false; }); changed(); }, done(l) ? '' : 'disabled'),
    item(icon('palette'), 'Change color', () => colorDialog(l)),
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
      h('span', { class: 'total-pill', 'aria-label': 'Total tasks', text: `${l.items.length} ${l.items.length === 1 ? 'task' : 'tasks'}` }),
      h('div', { class: 'menu-wrap' }, more)),
    title,
    h('p', { class: 'label', text: l.items.length ? `${remaining(l)} to do · ${done(l)} done` : 'No tasks' }),
    l.items.length ? h('div', { class: 'mt14' }, progressBar(l)) : null,
    h('div', { class: 'tasks' },
      !active.length ? (completed.length
        ? h('div', { class: 'empty' }, h('div', { class: 'big' }, icon('task_alt')), h('h3', { text: 'All done!' }), h('p', { text: 'Nothing left to do on this list.' }))
        : h('div', { class: 'empty' }, h('div', { class: 'big' }, icon('edit_note')), h('h3', { text: 'No tasks yet' }), h('p', { text: 'Tap “Add task”, type, and press Enter for the next one.' }))) : null,
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
      : r.name === 'all' ? allView()
      : r.name === 'settings' ? settingsView()
        : h('div', { class: 'pane-inner' }, h('div', { class: 'empty' }, h('div', { class: 'big' }, icon('checklist')), h('h3', { text: 'Pick a list' }), h('p', { text: 'Choose one on the left, or make a new one.' }))));
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
