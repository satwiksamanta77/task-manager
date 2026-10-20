/* ============================================================
 * TaskManager web client — vanilla JS SPA, no build step.
 * Talks to the Cloudflare Worker API per api-contract.md.
 * ============================================================ */

'use strict';

var API_BASE = 'https://taskmanager-api.satwiksamanta77.workers.dev/api';
var TOKEN_KEY = 'tm_token';

/* ---------------- utils ---------------- */

function esc(s) {
  return String(s == null ? '' : s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function pad(n) { return String(n).padStart(2, '0'); }

// "Oct 25, 10:00 AM" — '' when null
function fmtDate(iso) {
  if (!iso) return '';
  var d = new Date(iso);
  if (isNaN(d)) return '';
  return d.toLocaleDateString(undefined, { month: 'short', day: 'numeric' }) +
    ', ' + d.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
}

// "Sat, Oct 25" — for schedule group headers
function fmtDay(iso) {
  if (!iso) return 'No date';
  var d = new Date(iso);
  if (isNaN(d)) return 'No date';
  return d.toLocaleDateString(undefined, { weekday: 'short', month: 'short', day: 'numeric' });
}

// ISO -> "2026-10-25T10:00" for datetime-local inputs
function toLocalInput(iso) {
  if (!iso) return '';
  var d = new Date(iso);
  if (isNaN(d)) return '';
  return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate()) +
    'T' + pad(d.getHours()) + ':' + pad(d.getMinutes());
}

// "2026-10-25T10:00" -> ISO string (or null when empty)
function fromLocalInput(v) {
  if (!v) return null;
  var d = new Date(v);
  return isNaN(d) ? null : d.toISOString();
}

function isOverdue(iso) {
  if (!iso) return false;
  return new Date(iso).getTime() < Date.now();
}

function toast(msg, kind) {
  kind = kind || 'info';
  var el = document.getElementById('toast');
  var icon = kind === 'ok' ? ICONS.ok : kind === 'err' ? ICONS.alert : ICONS.info;
  el.innerHTML = '<span class="toast-icon ' + kind + '">' + icon + '</span><span>' + esc(msg) + '</span>';
  el.className = 'toast show ' + kind;
  el.hidden = false;
  clearTimeout(toast._t);
  toast._t = setTimeout(function () {
    el.classList.remove('show');
    setTimeout(function () { el.hidden = true; }, 260);
  }, 3200);
}

/* skeleton shimmer blocks while content loads */
function skel(n, cls) {
  var out = '';
  for (var i = 0; i < n; i++) out += '<div class="skel ' + cls + '"></div>';
  return out;
}

/* illustrated empty state — no emoji */
function emptyHtml(icon, title, copy, extra) {
  return '<div class="empty"><div class="empty-ill">' + icon + '</div>' +
    '<h3>' + esc(title) + '</h3><p>' + esc(copy) + '</p>' + (extra || '') + '</div>';
}

function getToken() { return localStorage.getItem(TOKEN_KEY); }

/* ---------------- api ---------------- */

function ApiError(status, message) {
  this.status = status;
  this.message = message;
}
ApiError.prototype = Object.create(Error.prototype);

function api(path, opts) {
  opts = opts || {};
  var headers = { 'Content-Type': 'application/json' };
  var token = getToken();
  if (token) headers['Authorization'] = 'Bearer ' + token;
  return fetch(API_BASE + path, {
    method: opts.method || 'GET',
    headers: headers,
    body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined
  }).then(function (res) {
    return res.json().catch(function () { return null; }).then(function (data) {
      if (!res.ok) {
        var onAuthPage = location.hash.indexOf('#/login') === 0 ||
                         location.hash.indexOf('#/register') === 0;
        if (res.status === 401 && !onAuthPage) {
          // Session invalid/expired: drop it and bounce to login.
          localStorage.removeItem(TOKEN_KEY);
          location.hash = '#/login';
          throw new ApiError(401, 'Session expired. Please log in again.');
        }
        throw new ApiError(res.status, (data && data.error) || ('Request failed (' + res.status + ')'));
      }
      return data;
    });
  });
}

/* ---------------- shell / nav ---------------- */

var ICONS = {
  tasks: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="3"/><path d="m8 12 3 3 6-6"/></svg>',
  notes: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M15.5 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V8.5L15.5 3Z"/><path d="M15 3v6h6"/></svg>',
  schedule: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="3" y="4" width="18" height="18" rx="3"/><path d="M16 2v4M8 2v4M3 10h18"/></svg>',
  profile: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 3.6-6.5 8-6.5s8 2.5 8 6.5"/></svg>',
  check: '<svg class="check" viewBox="0 0 24 24"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg>',
  mark: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="M5 12.5l4.5 4.5L19 7.5"/></svg>',
  plus: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M12 5v14M5 12h14"/></svg>',
  search: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="11" cy="11" r="7"/><path d="M20 20l-3.8-3.8"/></svg>',
  cal: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><rect x="3" y="5" width="18" height="16" rx="3"/><path d="M8 3v4M16 3v4M3 10.5h18"/></svg>',
  clock: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="8.5"/><path d="M12 7.5V12l3 2.5"/></svg>',
  pin: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 4h6l1 7 3 3v2H5v-2l3-3z"/><path d="M12 16v5"/></svg>',
  trash: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M4 7h16M10 7V5a1 1 0 0 1 1-1h2a1 1 0 0 1 1 1v2m3 0-.8 12.2a1 1 0 0 1-1 .8H8.8a1 1 0 0 1-1-.8L7 7"/></svg>',
  x: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round"><path d="M6 6l12 12M18 6L6 18"/></svg>',
  alert: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="9"/><path d="M12 8v5M12 16.5v.01"/></svg>',
  info: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round"><circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 7.5v.01"/></svg>',
  ok: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M8.5 12.5l2.5 2.5 4.5-5.5"/></svg>',
  projects: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 7H12l-2-2H4a2 2 0 0 0-2 2v10a2 2 0 0 0 2 2h16a2 2 0 0 0 2-2V9a2 2 0 0 0-2-2Z"/></svg>',
  tag: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.6 13.4 11 3.8A2 2 0 0 0 9.6 3.2H4a1 1 0 0 0-1 1v5.6c0 .5.2 1 .6 1.4l9.6 9.6a2 2 0 0 0 2.8 0l4.6-4.6a2 2 0 0 0 0-2.8Z"/><circle cx="7.5" cy="7.5" r="1.5"/></svg>',
  repeat: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 2l4 4-4 4"/><path d="M3 11v-1a4 4 0 0 1 4-4h14"/><path d="M7 22l-4-4 4-4"/><path d="M21 13v1a4 4 0 0 1-4 4H3"/></svg>',
  restore: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 12a9 9 0 1 0 2.6-6.3"/><path d="M3 4v5h5"/></svg>',
  listcheck: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="m3 17 2 2 4-4"/><path d="m3 7 2 2 4-4"/><path d="M13 6h8M13 12h8M13 18h8"/></svg>',
  edit: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 3a2.8 2.8 0 1 1 4 4L7.5 20.5 2 22l1.5-5.5Z"/></svg>'
};

var NAV = [
  { id: 'tasks', label: 'Tasks', href: '#/tasks' },
  { id: 'notes', label: 'Notes', href: '#/notes' },
  { id: 'projects', label: 'Projects', href: '#/projects' },
  { id: 'schedule', label: 'Schedule', href: '#/schedule' },
  { id: 'trash', label: 'Trash', href: '#/trash' },
  { id: 'profile', label: 'Profile', href: '#/profile' }
];

function navHtml(active) {
  return NAV.map(function (n) {
    return '<a class="nav-item' + (n.id === active ? ' active' : '') + '" href="' + n.href + '">' +
      ICONS[n.id] + '<span>' + n.label + '</span></a>';
  }).join('');
}

function shell(active, content) {
  return '<div class="layout">' +
    '<aside class="sidenav"><div class="brand-lockup"><span class="brand-mark">' + ICONS.mark + '</span><span class="brand-name">TaskManager</span></div>' +
    '<nav>' + navHtml(active) + '</nav></aside>' +
    '<div class="main"><header class="topbar"><div class="gsearch">' + ICONS.search +
    '<input id="gs" type="search" placeholder="Search tasks &amp; notes…" autocomplete="off" aria-label="Search tasks and notes">' +
    '<kbd title="Press / to search">/</kbd>' +
    '<div class="search-results" id="sr" hidden></div></div></header>' +
    '<main class="container">' + content + '</main></div>' +
    '<nav class="bottomnav">' + navHtml(active) + '</nav>' +
    '</div>';
}

/* ---------------- universal search (contract v2: GET /api/search) ---------------- */

var searchTimer = null;

function searchSnippet(text, q) {
  text = text || '';
  var i = text.toLowerCase().indexOf(q.toLowerCase());
  if (i === -1) return esc(text.slice(0, 90)) + (text.length > 90 ? '…' : '');
  var s = Math.max(0, i - 30);
  return (s > 0 ? '…' : '') + esc(text.slice(s, i + q.length + 50)) + '…';
}

function hideSearchResults() {
  var sr = document.getElementById('sr');
  if (sr) sr.hidden = true;
}

function runGlobalSearch(q) {
  var sr = document.getElementById('sr');
  if (!sr) return;
  api('/search?q=' + encodeURIComponent(q)).then(function (data) {
    if (!document.getElementById('sr')) return;
    var tasks = data.tasks || [], notes = data.notes || [];
    var html = '';
    if (tasks.length) {
      html += '<div class="sr-group">Tasks</div>' + tasks.map(function (t) {
        return '<button type="button" class="sr-row" data-kind="task" data-id="' + esc(t.id) + '">' +
          '<span class="sr-ic">' + ICONS.tasks + '</span>' +
          '<span><strong>' + esc(t.title) + '</strong>' +
          '<span class="sr-snip">' + searchSnippet(t.description || '', q) + '</span></span></button>';
      }).join('');
    }
    if (notes.length) {
      html += '<div class="sr-group">Notes</div>' + notes.map(function (n) {
        return '<button type="button" class="sr-row" data-kind="note" data-id="' + esc(n.id) + '">' +
          '<span class="sr-ic">' + ICONS.notes + '</span>' +
          '<span><strong>' + esc(n.title || 'Untitled') + '</strong>' +
          '<span class="sr-snip">' + searchSnippet(n.content || '', q) + '</span></span></button>';
      }).join('');
    }
    if (!html) html = '<div class="sr-empty">No results for &ldquo;' + esc(q) + '&rdquo;</div>';
    sr.innerHTML = html;
    sr.hidden = false;
  }).catch(function () { hideSearchResults(); });
}

/* delegated once at boot — the topbar is re-mounted on every route change */
function initGlobalSearch() {
  document.addEventListener('input', function (e) {
    if (!e.target || e.target.id !== 'gs') return;
    var q = e.target.value.trim();
    clearTimeout(searchTimer);
    if (q.length < 2) { hideSearchResults(); return; }
    searchTimer = setTimeout(function () { runGlobalSearch(q); }, 300);
  });
  document.addEventListener('keydown', function (e) {
    var ae = document.activeElement;
    var tag = (ae && ae.tagName) || '';
    var typing = /^(INPUT|TEXTAREA|SELECT)$/.test(tag) || (ae && ae.isContentEditable);
    if (e.key === '/' && !typing && !e.metaKey && !e.ctrlKey && !e.altKey) {
      var gs = document.getElementById('gs');
      if (gs && getToken()) { e.preventDefault(); gs.focus(); }
      return;
    }
    if (e.key === 'Escape') hideSearchResults();
  });
  // mousedown fires before blur, so row clicks register
  document.addEventListener('mousedown', function (e) {
    var row = e.target && e.target.closest ? e.target.closest('.sr-row') : null;
    if (row) {
      e.preventDefault();
      var kind = row.getAttribute('data-kind');
      var id = row.getAttribute('data-id');
      hideSearchResults();
      location.hash = (kind === 'task' ? '#/tasks/' : '#/notes/') + encodeURIComponent(id);
    }
  });
  document.addEventListener('click', function (e) {
    if (e.target && e.target.closest && !e.target.closest('.gsearch')) hideSearchResults();
  });
}

function authShell(title, subtitle, bodyHtml) {
  return '<div class="auth">' +
    '<aside class="auth-brand"><div class="mesh" aria-hidden="true"></div>' +
    '<div class="auth-brand-inner">' +
    '<div class="brand-lockup"><span class="brand-mark">' + ICONS.mark + '</span>' +
    '<span class="brand-name">TaskManager</span></div>' +
    '<h1>Every task, note and deadline.<br><span>One calm place.</span></h1>' +
    '<p class="lede">Tasks with subtasks, Keep-style notes and a smart schedule — synced instantly across your devices.</p>' +
    '<ul class="feat">' +
    '<li><span class="fi">' + ICONS.tasks + '</span><div><strong>Tasks &amp; subtasks</strong><span>Priorities, due dates and checklists</span></div></li>' +
    '<li><span class="fi">' + ICONS.notes + '</span><div><strong>Keep-style notes</strong><span>Colored cards, pinning and archive</span></div></li>' +
    '<li><span class="fi">' + ICONS.schedule + '</span><div><strong>Smart schedule</strong><span>Everything due, grouped by day</span></div></li>' +
    '</ul></div></aside>' +
    '<div class="auth-main"><div class="auth-card">' +
    '<h2>' + esc(title) + '</h2><p class="sub">' + esc(subtitle) + '</p>' +
    bodyHtml +
    '</div></div></div>';
}

function mount(html) {
  document.getElementById('app').innerHTML = html;
  window.scrollTo(0, 0);
}

function errBanner(msg) {
  return msg ? '<div class="error-banner">' + esc(msg) + '</div>' : '';
}

/* ---------------- router ---------------- */

var routes = [
  { re: /^#\/login\/?$/, view: LoginView, pub: true, nav: null },
  { re: /^#\/register\/?$/, view: RegisterView, pub: true, nav: null },
  { re: /^#\/tasks\/new\/?$/, view: TaskNewView, pub: false, nav: 'tasks' },
  { re: /^#\/tasks\/([^\/]+)\/?$/, view: TaskDetailView, pub: false, nav: 'tasks' },
  { re: /^#\/tasks\/?$/, view: TasksView, pub: false, nav: 'tasks' },
  { re: /^#\/notes\/new\/?$/, view: NoteEditorView, pub: false, nav: 'notes' },
  { re: /^#\/notes\/([^\/]+)\/?$/, view: NoteEditorView, pub: false, nav: 'notes' },
  { re: /^#\/notes\/?$/, view: NotesView, pub: false, nav: 'notes' },
  { re: /^#\/projects\/?$/, view: ProjectsView, pub: false, nav: 'projects' },
  { re: /^#\/trash\/?$/, view: TrashView, pub: false, nav: 'trash' },
  { re: /^#\/schedule\/?$/, view: ScheduleView, pub: false, nav: 'schedule' },
  { re: /^#\/profile\/?$/, view: ProfileView, pub: false, nav: 'profile' }
];

function router() {
  var hash = location.hash || '#/';
  var token = getToken();
  var match = null, params = [];
  for (var i = 0; i < routes.length; i++) {
    var m = routes[i].re.exec(hash);
    if (m) { match = routes[i]; params = m.slice(1); break; }
  }
  if (!match) { location.hash = token ? '#/tasks' : '#/login'; return; }
  if (!match.pub && !token) { location.hash = '#/login'; return; }
  if (match.pub && token) { location.hash = '#/tasks'; return; }
  match.view.apply(null, params.map(decodeURIComponent)).catch(function (e) {
    if (e instanceof ApiError) toast(e.message, 'err');
    else { console.error(e); toast('Something went wrong.', 'err'); }
  });
}

window.addEventListener('hashchange', router);

/* ---------------- auth views ---------------- */

function LoginView() {
  mount(authShell('Welcome back', 'Log in to sync your tasks and notes.', '' +
    '<div id="err"></div>' +
    '<div class="field"><label for="email">Email</label>' +
    '<input id="email" type="email" autocomplete="email" placeholder="you@example.com"></div>' +
    '<div class="field"><label for="password">Password</label>' +
    '<input id="password" type="password" autocomplete="current-password" placeholder="••••••••"></div>' +
    '<button class="btn btn-primary" id="go">Log in</button>' +
    '<div class="auth-switch">New here? <a href="#/register">Create an account</a></div>'));

  function submit() {
    var email = document.getElementById('email').value.trim();
    var password = document.getElementById('password').value;
    var btn = document.getElementById('go');
    btn.disabled = true;
    api('/auth/login', { method: 'POST', body: { email: email, password: password } })
      .then(function (data) {
        localStorage.setItem(TOKEN_KEY, data.token);
        location.hash = '#/tasks';
      })
      .catch(function (e) {
        document.getElementById('err').innerHTML = errBanner(e.message);
        btn.disabled = false;
      });
  }
  document.getElementById('go').addEventListener('click', submit);
  document.getElementById('password').addEventListener('keydown', function (e) {
    if (e.key === 'Enter') submit();
  });
  return Promise.resolve();
}

function RegisterView() {
  mount(authShell('Create account', 'One account for tasks, notes and schedule.', '' +
    '<div id="err"></div>' +
    '<div class="field"><label for="name">Name</label>' +
    '<input id="name" type="text" autocomplete="name" placeholder="Your name"></div>' +
    '<div class="field"><label for="email">Email</label>' +
    '<input id="email" type="email" autocomplete="email" placeholder="you@example.com"></div>' +
    '<div class="field"><label for="password">Password</label>' +
    '<input id="password" type="password" autocomplete="new-password" placeholder="Min. 6 characters"></div>' +
    '<button class="btn btn-primary" id="go">Create account</button>' +
    '<div class="auth-switch">Already have an account? <a href="#/login">Log in</a></div>'));

  function submit() {
    var name = document.getElementById('name').value.trim();
    var email = document.getElementById('email').value.trim();
    var password = document.getElementById('password').value;
    var btn = document.getElementById('go');
    btn.disabled = true;
    api('/auth/register', { method: 'POST', body: { name: name, email: email, password: password } })
      .then(function (data) {
        localStorage.setItem(TOKEN_KEY, data.token);
        location.hash = '#/tasks';
      })
      .catch(function (e) {
        document.getElementById('err').innerHTML = errBanner(e.message);
        btn.disabled = false;
      });
  }
  document.getElementById('go').addEventListener('click', submit);
  document.getElementById('password').addEventListener('keydown', function (e) {
    if (e.key === 'Enter') submit();
  });
  return Promise.resolve();
}

/* ---------------- tasks ---------------- */

var TASK_FILTERS = [
  { id: 'all', label: 'All' },
  { id: 'todo', label: 'To do' },
  { id: 'done', label: 'Done' }
];
var taskFilter = 'all';

/* ---------------- projects, labels, recurrence (contract v2) ---------------- */

var COLOR_SWATCHES = ['#58A6FF', '#3FB950', '#D29922', '#F85149', '#818CF8', '#DB61A2', '#F0883E', '#A371F7'];
var RECURRENCE = [
  { id: 'none', label: 'None' },
  { id: 'daily', label: 'Daily' },
  { id: 'weekly', label: 'Weekly' },
  { id: 'monthly', label: 'Monthly' }
];

var projectsCache = null;
var labelsCache = null;
var clCache = {}; // noteId -> checklist items
var taskProjectFilter = '';
var taskLabelFilter = '';
var noteLabelFilter = '';

function loadProjects() {
  if (projectsCache) return Promise.resolve(projectsCache);
  return api('/projects').then(function (d) { projectsCache = d.projects || []; return projectsCache; });
}
function loadLabels() {
  if (labelsCache) return Promise.resolve(labelsCache);
  return api('/labels').then(function (d) { labelsCache = d.labels || []; return labelsCache; });
}
function projectById(pid) {
  return (projectsCache || []).filter(function (p) { return p.id === pid; })[0] || null;
}
function projectChip(pid) {
  var p = projectById(pid);
  if (!p) return '';
  return '<span class="proj-chip"><span class="ld" style="background:' + esc(p.color || '#58A6FF') + '"></span>' + esc(p.name) + '</span>';
}
function labelPills(labels) {
  labels = labels || [];
  if (!labels.length) return '';
  return '<span class="label-pills">' + labels.map(function (l) {
    return '<span class="label-pill"><span class="ld" style="background:' + esc(l.color || '#7D8590') + '"></span>' + esc(l.name) + '</span>';
  }).join('') + '</span>';
}
function recurBadge(r) {
  if (!r || r === 'none') return '';
  return '<span class="recur-badge">' + ICONS.repeat + esc(r.charAt(0).toUpperCase() + r.slice(1)) + '</span>';
}
/* multi-select label chips for forms; prefix keeps element ids unique per form */
function labelChipsHtml(selected, prefix) {
  selected = selected || {};
  var ls = labelsCache || [];
  return '<div class="mchips" id="' + prefix + '-lchips">' +
    ls.map(function (l) {
      return '<button type="button" class="mchip' + (selected[l.id] ? ' selected' : '') + '" data-lid="' + esc(l.id) + '">' +
        '<span class="ld" style="background:' + esc(l.color || '#7D8590') + '"></span>' + esc(l.name) + '</button>';
    }).join('') +
    (ls.length ? '' : '<span class="hint">No labels yet — create some in Projects.</span>') +
    '</div>';
}
function bindLabelChips(prefix) {
  var box = document.getElementById(prefix + '-lchips');
  if (!box) return;
  box.addEventListener('click', function (e) {
    var b = e.target.closest('[data-lid]');
    if (b) b.classList.toggle('selected');
  });
}
function collectLabelIds(prefix) {
  var out = [];
  document.querySelectorAll('#' + prefix + '-lchips .mchip.selected').forEach(function (b) {
    out.push(b.getAttribute('data-lid'));
  });
  return out;
}
function swatchPickerHtml(picked, prefix) {
  return '<div class="swatches" id="' + prefix + '-sw">' + COLOR_SWATCHES.map(function (c) {
    return '<button type="button" class="swatch' + (c === picked ? ' selected' : '') + '" data-c="' + c +
      '" style="background:' + c + '" aria-label="Color ' + c + '"></button>';
  }).join('') + '</div>';
}
function bindSwatchPicker(prefix, onPick) {
  var box = document.getElementById(prefix + '-sw');
  if (!box) return;
  box.addEventListener('click', function (e) {
    var s = e.target.closest('[data-c]');
    if (!s) return;
    var c = s.getAttribute('data-c');
    box.querySelectorAll('.swatch').forEach(function (el) {
      el.classList.toggle('selected', el.getAttribute('data-c') === c);
    });
    onPick(c);
  });
}
function pickedSwatch(prefix) {
  var el = document.querySelector('#' + prefix + '-sw .swatch.selected');
  return el ? el.getAttribute('data-c') : null;
}
/* segmented single-choice (recurrence, note mode) */
function segHtml(options, current, id) {
  return '<div class="chips flat" id="' + id + '">' + options.map(function (o) {
    return '<button type="button" class="chip' + (o.id === current ? ' active' : '') + '" data-v="' + o.id + '">' + o.label + '</button>';
  }).join('') + '</div>';
}
function bindSeg(id, onPick) {
  var box = document.getElementById(id);
  if (!box) return;
  box.addEventListener('click', function (e) {
    var b = e.target.closest('[data-v]');
    if (!b) return;
    var v = b.getAttribute('data-v');
    box.querySelectorAll('.chip').forEach(function (c) {
      c.classList.toggle('active', c.getAttribute('data-v') === v);
    });
    onPick(v);
  });
}

function priorityPill(p) {
  var cls = p === 'high' ? 'pill-high' : p === 'low' ? 'pill-low' : 'pill-medium';
  return '<span class="pill ' + cls + '">' + esc(p || 'medium') + '</span>';
}

function taskRow(t) {
  var due = t.due_at ? fmtDate(t.due_at) : '';
  var overdue = t.status !== 'done' && isOverdue(t.due_at);
  var done = t.status === 'done';
  return '<div class="task-row' + (done ? ' done' : '') + '" data-id="' + esc(t.id) + '">' +
    '<button type="button" class="checkbox" data-toggle="' + esc(t.id) + '" aria-pressed="' + done + '"' +
    ' aria-label="' + (done ? 'Mark as to do' : 'Mark as done') + '">' + ICONS.check + '</button>' +
    '<div class="task-main"><div class="task-title">' + esc(t.title) + '</div>' +
    '<div class="task-meta">' + priorityPill(t.priority) +
    projectChip(t.project_id) + recurBadge(t.recurrence) +
    (due ? '<span class="due' + (overdue ? ' overdue' : '') + '">' + ICONS.cal + esc(due) + '</span>' : '') +
    labelPills(t.labels) +
    '</div></div></div>';
}

function TasksView() {
  mount(shell('tasks',
    '<div class="page-head"><h2>My <span class="grad-text">Tasks</span></h2></div>' +
    '<div class="chips" id="filters">' +
    TASK_FILTERS.map(function (f) {
      return '<button class="chip' + (f.id === taskFilter ? ' active' : '') + '" data-f="' + f.id + '">' + f.label + '</button>';
    }).join('') + '</div>' +
    '<div class="filterbar" id="filterbar"></div>' +
    '<div class="list" id="list">' + skel(4, 'skel-row') + '</div>' +
    '<button class="fab" id="fab" aria-label="Add task">' + ICONS.plus + '</button>'));

  function renderFilterbar() {
    var fb = document.getElementById('filterbar');
    if (!fb) return;
    var html = '<select id="projf" aria-label="Filter by project"><option value="">All projects</option>' +
      projectsCache.map(function (p) {
        return '<option value="' + esc(p.id) + '"' + (p.id === taskProjectFilter ? ' selected' : '') + '>' + esc(p.name) + '</option>';
      }).join('') + '</select>';
    if (labelsCache.length) {
      html += '<div class="mchips" id="tlabelf">' +
        '<button type="button" class="mchip' + (!taskLabelFilter ? ' selected' : '') + '" data-lid="">All</button>' +
        labelsCache.map(function (l) {
          return '<button type="button" class="mchip' + (l.id === taskLabelFilter ? ' selected' : '') + '" data-lid="' + esc(l.id) + '">' +
            '<span class="ld" style="background:' + esc(l.color || '#7D8590') + '"></span>' + esc(l.name) + '</button>';
        }).join('') + '</div>';
    }
    fb.innerHTML = html;
    document.getElementById('projf').addEventListener('change', function (e) {
      taskProjectFilter = e.target.value;
      load().catch(function (err) { toast(err.message, 'err'); });
    });
    var lf = document.getElementById('tlabelf');
    if (lf) lf.addEventListener('click', function (e) {
      var b = e.target.closest('[data-lid]');
      if (!b) return;
      taskLabelFilter = b.getAttribute('data-lid');
      lf.querySelectorAll('.mchip').forEach(function (c) {
        c.classList.toggle('selected', c.getAttribute('data-lid') === taskLabelFilter);
      });
      load().catch(function (err) { toast(err.message, 'err'); });
    });
  }

  function load() {
    var q = '?';
    if (taskFilter !== 'all') q += 'status=' + taskFilter + '&';
    if (taskProjectFilter) q += 'project_id=' + encodeURIComponent(taskProjectFilter) + '&';
    if (taskLabelFilter) q += 'label_id=' + encodeURIComponent(taskLabelFilter) + '&';
    return api('/tasks' + q).then(function (data) {
      var list = document.getElementById('list');
      if (!list) return;
      if (!data.tasks.length) {
        list.innerHTML = emptyHtml(ICONS.tasks, 'All clear',
          'No tasks match these filters. Tap + to capture a new one.');
        return;
      }
      list.innerHTML = data.tasks.map(taskRow).join('');
    });
  }

  document.getElementById('filters').addEventListener('click', function (e) {
    var b = e.target.closest('[data-f]');
    if (!b) return;
    taskFilter = b.getAttribute('data-f');
    document.querySelectorAll('#filters .chip').forEach(function (c) {
      c.classList.toggle('active', c.getAttribute('data-f') === taskFilter);
    });
    load().catch(function (err) { toast(err.message, 'err'); });
  });

  document.getElementById('list').addEventListener('click', function (e) {
    var toggle = e.target.closest('[data-toggle]');
    if (toggle) {
      e.stopPropagation();
      var id = toggle.getAttribute('data-toggle');
      var done = toggle.getAttribute('aria-pressed') !== 'true';
      toggle.setAttribute('aria-pressed', done ? 'true' : 'false');
      toggle.setAttribute('aria-label', done ? 'Mark as to do' : 'Mark as done');
      api('/tasks/' + encodeURIComponent(id), {
        method: 'PUT', body: { status: done ? 'done' : 'todo' }
      }).then(function (data) {
        if (data && data.next_task) toast('Done — next occurrence created.', 'ok');
        load();
      }).catch(function (err) { toast(err.message, 'err'); load(); });
      return;
    }
    var row = e.target.closest('.task-row');
    if (row) location.hash = '#/tasks/' + encodeURIComponent(row.getAttribute('data-id'));
  });

  document.getElementById('fab').addEventListener('click', function () {
    location.hash = '#/tasks/new';
  });

  return Promise.all([loadProjects(), loadLabels()]).then(function () {
    renderFilterbar();
    return load();
  });
}

function TaskNewView() {
  mount(shell('tasks',
    '<div class="page-head"><h2>New <span class="grad-text">Task</span></h2></div>' +
    '<div class="card">' + skel(5, 'skel-line') + '</div>'));

  return Promise.all([loadProjects(), loadLabels()]).then(function () {
    mount(shell('tasks',
      '<div class="page-head"><h2>New <span class="grad-text">Task</span></h2></div>' +
      '<div class="card"><div class="form">' +
      '<div id="err"></div>' +
      '<div class="field"><label for="title">Title</label>' +
      '<input id="title" type="text" placeholder="What needs doing?"></div>' +
      '<div class="field"><label for="desc">Description</label>' +
      '<textarea id="desc" placeholder="Details (optional)"></textarea></div>' +
      '<div class="field"><label for="project">Project</label><select id="project">' +
      '<option value="">No project</option>' +
      projectsCache.map(function (p) {
        return '<option value="' + esc(p.id) + '">' + esc(p.name) + '</option>';
      }).join('') + '</select></div>' +
      '<div class="field"><label for="priority">Priority</label>' +
      '<select id="priority"><option value="low">Low</option>' +
      '<option value="medium" selected>Medium</option><option value="high">High</option></select></div>' +
      '<div class="field"><label>Repeats</label>' + segHtml(RECURRENCE, 'none', 'recur') + '</div>' +
      '<div class="field"><label>Labels</label>' + labelChipsHtml({}, 'nt') + '</div>' +
      '<div class="field"><label for="due">Due date</label>' +
      '<input id="due" type="datetime-local"></div>' +
      '<div class="field"><label for="rem">Reminder</label>' +
      '<input id="rem" type="datetime-local"></div>' +
      '<div class="form-row"><a class="btn btn-ghost" href="#/tasks" style="text-align:center">Cancel</a>' +
      '<button class="btn btn-primary" id="save">Create task</button></div>' +
      '</div></div>'));

    var recurrence = 'none';
    bindSeg('recur', function (v) { recurrence = v; });
    bindLabelChips('nt');

    document.getElementById('save').addEventListener('click', function () {
      var btn = this;
      var title = document.getElementById('title').value.trim();
      if (!title) {
        document.getElementById('err').innerHTML = errBanner('Please enter a title.');
        return;
      }
      btn.disabled = true;
      api('/tasks', {
        method: 'POST',
        body: {
          title: title,
          description: document.getElementById('desc').value.trim() || null,
          priority: document.getElementById('priority').value,
          project_id: document.getElementById('project').value || null,
          recurrence: recurrence,
          label_ids: collectLabelIds('nt'),
          due_at: fromLocalInput(document.getElementById('due').value),
          reminder_at: fromLocalInput(document.getElementById('rem').value)
        }
      }).then(function (data) {
        location.hash = '#/tasks/' + encodeURIComponent(data.task.id);
      }).catch(function (e) {
        document.getElementById('err').innerHTML = errBanner(e.message);
        btn.disabled = false;
      });
    });
  });
}

function TaskDetailView(id) {
  mount(shell('tasks', '<div class="page-head"><h2>Task <span class="grad-text">details</span></h2></div>' +
    '<div class="card">' + skel(5, 'skel-line') + '</div>'));

  return Promise.all([
    api('/tasks/' + encodeURIComponent(id)),
    loadProjects(),
    loadLabels()
  ]).then(function (res) {
    var t = res[0].task;
    var subs = t.subtasks || [];
    var selLabels = {};
    (t.labels || []).forEach(function (l) { selLabels[l.id] = true; });
    var recurrence = t.recurrence || 'none';
    mount(shell('tasks',
      '<div class="page-head"><h2>Edit <span class="grad-text">task</span></h2></div>' +
      '<div class="card"><div class="form">' +
      '<div id="err"></div>' +
      '<div class="field"><label for="title">Title</label>' +
      '<input id="title" type="text" value="' + esc(t.title) + '"></div>' +
      '<div class="field"><label for="desc">Description</label>' +
      '<textarea id="desc">' + esc(t.description || '') + '</textarea></div>' +
      '<div class="field"><label for="project">Project</label><select id="project">' +
      '<option value="">No project</option>' +
      projectsCache.map(function (p) {
        return '<option value="' + esc(p.id) + '"' + (p.id === t.project_id ? ' selected' : '') + '>' + esc(p.name) + '</option>';
      }).join('') + '</select></div>' +
      '<div class="field"><label for="status">Status</label><select id="status">' +
      '<option value="todo"' + (t.status === 'todo' ? ' selected' : '') + '>To do</option>' +
      '<option value="done"' + (t.status === 'done' ? ' selected' : '') + '>Done</option></select></div>' +
      '<div class="field"><label for="priority">Priority</label><select id="priority">' +
      ['low', 'medium', 'high'].map(function (p) {
        return '<option value="' + p + '"' + (t.priority === p ? ' selected' : '') + '>' +
          p.charAt(0).toUpperCase() + p.slice(1) + '</option>';
      }).join('') + '</select></div>' +
      '<div class="field"><label>Repeats</label>' + segHtml(RECURRENCE, recurrence, 'recur') + '</div>' +
      '<div class="field"><label>Labels</label>' + labelChipsHtml(selLabels, 'td') + '</div>' +
      '<div class="field"><label for="due">Due date</label>' +
      '<input id="due" type="datetime-local" value="' + esc(toLocalInput(t.due_at)) + '"></div>' +
      '<div class="field"><label for="rem">Reminder</label>' +
      '<input id="rem" type="datetime-local" value="' + esc(toLocalInput(t.reminder_at)) + '"></div>' +
      '<button class="btn btn-primary" id="save">Save changes</button>' +
      '</div>' +

      '<div class="section-title">Subtasks (' + subs.length + ')</div>' +
      '<div class="subtask-list" id="subs">' +
      subs.map(function (s) {
        return '<div class="subtask-row"><button type="button" class="checkbox" data-sub="' + esc(s.id) + '"' +
          ' aria-pressed="' + !!s.done + '" aria-label="Toggle subtask">' + ICONS.check + '</button>' +
          '<span class="' + (s.done ? 'done' : '') + '">' + esc(s.title) + '</span>' +
          '<button class="icon-btn" data-delsub="' + esc(s.id) + '" aria-label="Delete subtask">' + ICONS.x + '</button></div>';
      }).join('') + '</div>' +
      '<div class="add-row" style="margin-top:10px"><input id="newsub" type="text" placeholder="Add a subtask…">' +
      '<button class="btn btn-ghost btn-sm" id="addsub">Add</button></div>' +

      '<div class="section-title">Danger zone</div>' +
      '<button class="btn btn-danger" id="del" style="width:100%">Delete task</button>' +
      '</div>'));

    function reload() { TaskDetailView(id); }

    bindSeg('recur', function (v) { recurrence = v; });
    bindLabelChips('td');

    document.getElementById('save').addEventListener('click', function () {
      var btn = this;
      var title = document.getElementById('title').value.trim();
      if (!title) {
        document.getElementById('err').innerHTML = errBanner('Title cannot be empty.');
        return;
      }
      btn.disabled = true;
      api('/tasks/' + encodeURIComponent(id), {
        method: 'PUT',
        body: {
          title: title,
          description: document.getElementById('desc').value.trim() || null,
          status: document.getElementById('status').value,
          priority: document.getElementById('priority').value,
          project_id: document.getElementById('project').value || null,
          recurrence: recurrence,
          label_ids: collectLabelIds('td'),
          due_at: fromLocalInput(document.getElementById('due').value),
          reminder_at: fromLocalInput(document.getElementById('rem').value)
        }
      }).then(function (data) {
        if (data && data.next_task) toast('Saved — next occurrence created.', 'ok');
        else toast('Saved.', 'ok');
        reload();
      })
        .catch(function (e) {
          document.getElementById('err').innerHTML = errBanner(e.message);
          btn.disabled = false;
        });
    });

    document.getElementById('subs').addEventListener('click', function (e) {
      var tg = e.target.closest('[data-sub]');
      if (tg) {
        var done = tg.getAttribute('aria-pressed') !== 'true';
        tg.setAttribute('aria-pressed', done ? 'true' : 'false');
        api('/subtasks/' + encodeURIComponent(tg.getAttribute('data-sub')), {
          method: 'PUT', body: { done: done }
        }).then(reload).catch(function (err) { toast(err.message, 'err'); });
        return;
      }
      var del = e.target.closest('[data-delsub]');
      if (del && confirm('Delete this subtask?')) {
        api('/subtasks/' + encodeURIComponent(del.getAttribute('data-delsub')), { method: 'DELETE' })
          .then(reload).catch(function (err) { toast(err.message, 'err'); });
      }
    });

    function addSub() {
      var inp = document.getElementById('newsub');
      var title = inp.value.trim();
      if (!title) return;
      api('/tasks/' + encodeURIComponent(id) + '/subtasks', { method: 'POST', body: { title: title } })
        .then(reload).catch(function (err) { toast(err.message, 'err'); });
    }
    document.getElementById('addsub').addEventListener('click', addSub);
    document.getElementById('newsub').addEventListener('keydown', function (e) {
      if (e.key === 'Enter') addSub();
    });

    document.getElementById('del').addEventListener('click', function () {
      if (!confirm('Delete this task and all its subtasks?')) return;
      api('/tasks/' + encodeURIComponent(id), { method: 'DELETE' })
        .then(function () { location.hash = '#/tasks'; })
        .catch(function (e) { toast(e.message, 'err'); });
    });
  }).catch(function (e) {
    if (e.status === 404) {
      mount(shell('tasks', emptyHtml(ICONS.search, 'Task not found',
        'This task may have been deleted.', '<a class="btn btn-ghost btn-sm" href="#/tasks">Back to tasks</a>')));
      return;
    }
    throw e;
  });
}

/* ---------------- notes (Keep style) ---------------- */

var NOTE_COLORS = ['#FFF7AE', '#F28B82', '#FBBC04', '#CCFF90', '#A7FFEB', '#CBF0F8', '#D7AEFB'];
var noteSearch = '';
var showArchived = false;

function noteCard(n) {
  var preview = (n.content || '').length > 140 ? n.content.slice(0, 140) + '…' : (n.content || '');
  return '<article class="note-card" data-id="' + esc(n.id) + '" style="background:' + esc(n.color || '#FFF7AE') + '">' +
    (n.pinned ? '<span class="pin-ribbon" aria-hidden="true">' + ICONS.pin + '</span>' : '') +
    '<div class="note-actions">' +
    '<button type="button" class="note-act" data-pin="' + esc(n.id) + '" data-pinned="' + (n.pinned ? '1' : '0') + '"' +
    ' aria-label="' + (n.pinned ? 'Unpin note' : 'Pin note') + '" title="' + (n.pinned ? 'Unpin' : 'Pin') + '">' + ICONS.pin + '</button>' +
    '<button type="button" class="note-act" data-del="' + esc(n.id) + '"' +
    ' aria-label="Delete note" title="Delete">' + ICONS.trash + '</button>' +
    '</div>' +
    (n.title ? '<h3>' + esc(n.title) + '</h3>' : '') +
    (preview ? '<p>' + esc(preview) + '</p>' : '') +
    labelPills(n.labels) +
    '<span class="clprog" data-clprog="' + esc(n.id) + '"></span>' +
    '</article>';
}

/* fetch checklist items per note (cached) and paint "x/y done" on cards */
function refreshChecklistProgress(notes) {
  (notes || []).forEach(function (n) {
    var paint = function (items) {
      if (!items.length) return;
      var done = items.filter(function (i) { return i.checked; }).length;
      var els = document.querySelectorAll('.clprog');
      for (var k = 0; k < els.length; k++) {
        if (els[k].getAttribute('data-clprog') === n.id) {
          els[k].innerHTML = ICONS.listcheck + '<span>' + done + '/' + items.length + ' done</span>';
        }
      }
    };
    if (clCache[n.id]) { paint(clCache[n.id]); return; }
    api('/notes/' + encodeURIComponent(n.id) + '/items').then(function (d) {
      clCache[n.id] = d.items || [];
      paint(clCache[n.id]);
    }).catch(function () { /* offline/unauthorized — leave blank */ });
  });
}

function NotesView() {
  mount(shell('notes',
    '<div class="page-head"><h2>My <span class="grad-text">Notes</span></h2></div>' +
    '<div class="searchbar">' + ICONS.search + '<input id="search" type="search" placeholder="Search notes…" value="' + esc(noteSearch) + '" aria-label="Search notes"></div>' +
    '<div class="chips">' +
    '<button class="chip' + (!showArchived ? ' active' : '') + '" data-arch="0">Notes</button>' +
    '<button class="chip' + (showArchived ? ' active' : '') + '" data-arch="1">Archived</button>' +
    '</div>' +
    '<div class="filterbar" id="nfilterbar"></div>' +
    '<div class="notes-masonry" id="grid">' + skel(6, 'skel-note') + '</div>' +
    '<button class="fab" id="fab" aria-label="Add note">' + ICONS.plus + '</button>'));

  var cache = [];

  function renderNoteFilterbar() {
    var fb = document.getElementById('nfilterbar');
    if (!fb) return;
    if (!labelsCache.length) { fb.innerHTML = ''; return; }
    fb.innerHTML = '<div class="mchips">' +
      '<button type="button" class="mchip' + (!noteLabelFilter ? ' selected' : '') + '" data-lid="">All</button>' +
      labelsCache.map(function (l) {
        return '<button type="button" class="mchip' + (l.id === noteLabelFilter ? ' selected' : '') + '" data-lid="' + esc(l.id) + '">' +
          '<span class="ld" style="background:' + esc(l.color || '#7D8590') + '"></span>' + esc(l.name) + '</button>';
      }).join('') + '</div>';
    fb.querySelector('.mchips').addEventListener('click', function (e) {
      var b = e.target.closest('[data-lid]');
      if (!b) return;
      noteLabelFilter = b.getAttribute('data-lid');
      fb.querySelectorAll('.mchip').forEach(function (c) {
        c.classList.toggle('selected', c.getAttribute('data-lid') === noteLabelFilter);
      });
      load().catch(function (err) { toast(err.message, 'err'); });
    });
  }

  function paint() {
    var grid = document.getElementById('grid');
    if (!grid) return;
    var q = noteSearch.trim().toLowerCase();
    var items = cache.filter(function (n) {
      return !q || (n.title + ' ' + n.content).toLowerCase().indexOf(q) !== -1;
    });
    grid.innerHTML = items.length ? items.map(noteCard).join('') :
      emptyHtml(ICONS.notes, q ? 'No matches' : 'No notes yet',
        q ? 'Try a different search term.'
          : 'Jot down ideas, lists and reminders. They sync instantly across your devices.');
    refreshChecklistProgress(items);
  }

  function load() {
    var q = '?archived=' + (showArchived ? 'true' : 'false');
    if (noteLabelFilter) q += '&label_id=' + encodeURIComponent(noteLabelFilter);
    return api('/notes' + q).then(function (data) {
      cache = data.notes || [];
      paint();
    });
  }

  document.getElementById('search').addEventListener('input', function (e) {
    noteSearch = e.target.value;
    paint();
  });

  document.querySelector('.chips').addEventListener('click', function (e) {
    var b = e.target.closest('[data-arch]');
    if (!b) return;
    showArchived = b.getAttribute('data-arch') === '1';
    document.querySelectorAll('.chips .chip').forEach(function (c, i) {
      c.classList.toggle('active', (i === 1) === showArchived);
    });
    load().catch(function (err) { toast(err.message, 'err'); });
  });

  document.getElementById('grid').addEventListener('click', function (e) {
    var pinBtn = e.target.closest('[data-pin]');
    if (pinBtn) {
      e.stopPropagation();
      var pid = pinBtn.getAttribute('data-pin');
      var pinned = pinBtn.getAttribute('data-pinned') === '1';
      api('/notes/' + encodeURIComponent(pid), { method: 'PUT', body: { pinned: !pinned } })
        .then(function () { toast(pinned ? 'Note unpinned.' : 'Note pinned to top.', 'ok'); load(); })
        .catch(function (err) { toast(err.message, 'err'); });
      return;
    }
    var delBtn = e.target.closest('[data-del]');
    if (delBtn) {
      e.stopPropagation();
      if (!confirm('Delete this note?')) return;
      api('/notes/' + encodeURIComponent(delBtn.getAttribute('data-del')), { method: 'DELETE' })
        .then(function () { toast('Note deleted.', 'ok'); load(); })
        .catch(function (err) { toast(err.message, 'err'); });
      return;
    }
    var card = e.target.closest('.note-card');
    if (card) location.hash = '#/notes/' + encodeURIComponent(card.getAttribute('data-id'));
  });

  document.getElementById('fab').addEventListener('click', function () {
    location.hash = '#/notes/new';
  });

  return loadLabels().then(function () {
    renderNoteFilterbar();
    return load();
  });
}

/* ---------------- projects & labels management (contract v2) ---------------- */

function ProjectsView() {
  mount(shell('projects',
    '<div class="page-head"><h2>My <span class="grad-text">Projects</span></h2></div>' +
    '<div class="card">' + skel(4, 'skel-line') + '</div>'));

  var editingProj = null;
  var editingLabel = null;
  var newProjColor = COLOR_SWATCHES[0];
  var newLabelColor = COLOR_SWATCHES[2];
  var counts = {};

  function refresh() {
    return Promise.all([api('/projects'), api('/labels'), api('/tasks')]).then(function (res) {
      projectsCache = res[0].projects || [];
      labelsCache = res[1].labels || [];
      counts = {};
      (res[2].tasks || []).forEach(function (t) {
        if (t.project_id) counts[t.project_id] = (counts[t.project_id] || 0) + 1;
      });
      render();
    });
  }

  function projRow(p) {
    if (editingProj === p.id) {
      return '<div class="proj-row"><div class="form" style="width:100%;gap:10px">' +
        '<input id="epname" type="text" value="' + esc(p.name) + '" placeholder="Project name">' +
        swatchPickerHtml(p.color || COLOR_SWATCHES[0], 'ep') +
        '<div class="form-row"><button type="button" class="btn btn-ghost btn-sm" id="epcancel">Cancel</button>' +
        '<button type="button" class="btn btn-primary btn-sm" id="epsave">Save</button></div></div></div>';
    }
    return '<div class="proj-row">' +
      '<span class="proj-dot" style="background:' + esc(p.color || '#58A6FF') + '"></span>' +
      '<span class="proj-name" data-goto-proj="' + esc(p.id) + '" title="Show its tasks">' + esc(p.name) + '</span>' +
      '<span class="pill pill-neutral">' + (counts[p.id] || 0) + ' tasks</span>' +
      '<button type="button" class="icon-btn" data-pedit="' + esc(p.id) + '" aria-label="Edit project" title="Edit">' + ICONS.edit + '</button>' +
      '<button type="button" class="icon-btn" data-pdel="' + esc(p.id) + '" aria-label="Delete project" title="Delete">' + ICONS.trash + '</button></div>';
  }

  function labelRow(l) {
    if (editingLabel === l.id) {
      return '<div class="proj-row"><div class="form" style="width:100%;gap:10px">' +
        '<input id="elname" type="text" value="' + esc(l.name) + '" placeholder="Label name">' +
        swatchPickerHtml(l.color || COLOR_SWATCHES[2], 'el') +
        '<div class="form-row"><button type="button" class="btn btn-ghost btn-sm" id="elcancel">Cancel</button>' +
        '<button type="button" class="btn btn-primary btn-sm" id="elsave">Save</button></div></div></div>';
    }
    return '<div class="proj-row">' +
      '<span class="proj-dot" style="background:' + esc(l.color || '#7D8590') + '"></span>' +
      '<span class="proj-name">' + esc(l.name) + '</span>' +
      '<button type="button" class="icon-btn" data-ledit="' + esc(l.id) + '" aria-label="Rename label" title="Rename">' + ICONS.edit + '</button>' +
      '<button type="button" class="icon-btn" data-ldel="' + esc(l.id) + '" aria-label="Delete label" title="Delete">' + ICONS.trash + '</button></div>';
  }

  function render() {
    mount(shell('projects',
      '<div class="page-head"><h2>My <span class="grad-text">Projects</span></h2></div>' +
      '<div class="card"><div class="section-title" style="margin-top:0">Projects</div>' +
      '<p class="hint" style="margin-bottom:12px">Group tasks into projects. Tap a project name to see its tasks.</p>' +
      '<div class="add-row" style="margin-top:0"><input id="newproj" type="text" placeholder="New project name…">' +
      '<button type="button" class="btn btn-primary btn-sm" id="addproj">Add</button></div>' +
      swatchPickerHtml(newProjColor, 'np') +
      '<div class="proj-list">' +
      (projectsCache.length ? projectsCache.map(projRow).join('') : '<p class="hint">No projects yet.</p>') +
      '</div></div>' +
      '<div class="card" style="margin-top:16px"><div class="section-title" style="margin-top:0">Labels</div>' +
      '<p class="hint" style="margin-bottom:12px">Labels organize both tasks and notes.</p>' +
      '<div class="add-row" style="margin-top:0"><input id="newlabel" type="text" placeholder="New label name…">' +
      '<button type="button" class="btn btn-primary btn-sm" id="addlabel">Add</button></div>' +
      swatchPickerHtml(newLabelColor, 'nl') +
      '<div class="proj-list">' +
      (labelsCache.length ? labelsCache.map(labelRow).join('') : '<p class="hint">No labels yet.</p>') +
      '</div></div>'));
    bind();
  }

  function addProject() {
    var inp = document.getElementById('newproj');
    var name = inp.value.trim();
    if (!name) { toast('Enter a project name.', 'err'); return; }
    api('/projects', { method: 'POST', body: { name: name, color: newProjColor } })
      .then(function () { toast('Project created.', 'ok'); editingProj = null; refresh(); })
      .catch(function (e) { toast(e.message, 'err'); });
  }

  function addLabel() {
    var inp = document.getElementById('newlabel');
    var name = inp.value.trim();
    if (!name) { toast('Enter a label name.', 'err'); return; }
    api('/labels', { method: 'POST', body: { name: name, color: newLabelColor } })
      .then(function () { toast('Label created.', 'ok'); refresh(); })
      .catch(function (e) { toast(e.message, 'err'); });
  }

  function bind() {
    bindSwatchPicker('np', function (c) { newProjColor = c; });
    bindSwatchPicker('nl', function (c) { newLabelColor = c; });
    document.getElementById('addproj').addEventListener('click', addProject);
    document.getElementById('newproj').addEventListener('keydown', function (e) {
      if (e.key === 'Enter') addProject();
    });
    document.getElementById('addlabel').addEventListener('click', addLabel);
    document.getElementById('newlabel').addEventListener('keydown', function (e) {
      if (e.key === 'Enter') addLabel();
    });
    // delegate row actions to the fresh container (replaced on every render)
    document.querySelector('.container').addEventListener('click', function (e) {
      var go = e.target.closest('[data-goto-proj]');
      if (go) {
        taskProjectFilter = go.getAttribute('data-goto-proj');
        location.hash = '#/tasks';
        return;
      }
      var pe = e.target.closest('[data-pedit]');
      if (pe) { editingProj = pe.getAttribute('data-pedit'); render(); return; }
      var pd = e.target.closest('[data-pdel]');
      if (pd) {
        if (!confirm('Delete this project? Its tasks will keep existing without a project.')) return;
        api('/projects/' + encodeURIComponent(pd.getAttribute('data-pdel')), { method: 'DELETE' })
          .then(function () { toast('Project deleted.', 'ok'); refresh(); })
          .catch(function (err) { toast(err.message, 'err'); });
        return;
      }
      var le = e.target.closest('[data-ledit]');
      if (le) { editingLabel = le.getAttribute('data-ledit'); render(); return; }
      var ld = e.target.closest('[data-ldel]');
      if (ld) {
        if (!confirm('Delete this label? It will be removed from all tasks and notes.')) return;
        api('/labels/' + encodeURIComponent(ld.getAttribute('data-ldel')), { method: 'DELETE' })
          .then(function () { toast('Label deleted.', 'ok'); refresh(); })
          .catch(function (err) { toast(err.message, 'err'); });
        return;
      }
      if (e.target.closest('#epcancel')) { editingProj = null; render(); return; }
      if (e.target.closest('#elcancel')) { editingLabel = null; render(); return; }
      if (e.target.closest('#epsave')) {
        var pn = document.getElementById('epname').value.trim();
        if (!pn) { toast('Project name cannot be empty.', 'err'); return; }
        api('/projects/' + encodeURIComponent(editingProj), {
          method: 'PUT', body: { name: pn, color: pickedSwatch('ep') || COLOR_SWATCHES[0] }
        }).then(function () { editingProj = null; toast('Project updated.', 'ok'); refresh(); })
          .catch(function (err) { toast(err.message, 'err'); });
        return;
      }
      if (e.target.closest('#elsave')) {
        var ln = document.getElementById('elname').value.trim();
        if (!ln) { toast('Label name cannot be empty.', 'err'); return; }
        api('/labels/' + encodeURIComponent(editingLabel), {
          method: 'PUT', body: { name: ln, color: pickedSwatch('el') || COLOR_SWATCHES[2] }
        }).then(function () { editingLabel = null; toast('Label updated.', 'ok'); refresh(); })
          .catch(function (err) { toast(err.message, 'err'); });
      }
    });
  }

  return refresh().catch(function (e) { toast(e.message, 'err'); });
}

/* ---------------- trash (contract v2: soft delete) ---------------- */

function trashRow(kind, id, title, sub) {
  return '<div class="trash-row"><div class="trash-info">' +
    '<div class="trash-kind">' + kind + '</div>' +
    '<div class="trash-title">' + esc(title) + '</div>' +
    (sub ? '<div class="trash-sub">' + esc(sub) + '</div>' : '') + '</div>' +
    '<div class="trash-actions">' +
    '<button type="button" class="btn btn-ghost btn-sm" data-restore="' + kind + ':' + esc(id) + '">' + ICONS.restore + 'Restore</button>' +
    '<button type="button" class="btn btn-danger btn-sm" data-perm="' + kind + ':' + esc(id) + '">Delete forever</button></div></div>';
}

function TrashView() {
  mount(shell('trash',
    '<div class="page-head"><h2><span class="grad-text">Trash</span></h2>' +
    '<button type="button" class="btn btn-danger btn-sm" id="emptytrash">Empty trash</button></div>' +
    '<div id="tlist">' + skel(4, 'skel-row') + '</div>'));

  function load() {
    return Promise.all([api('/tasks?trashed=true'), api('/notes?trashed=true')]).then(function (res) {
      var tasks = res[0].tasks || [];
      var notes = res[1].notes || [];
      var list = document.getElementById('tlist');
      if (!list) return;
      var emptyBtn = document.getElementById('emptytrash');
      if (!tasks.length && !notes.length) {
        list.innerHTML = emptyHtml(ICONS.trash, 'Trash is empty',
          'Deleted tasks and notes appear here for 30 days of grace — restore them or delete them forever.');
        if (emptyBtn) emptyBtn.style.display = 'none';
        return;
      }
      if (emptyBtn) emptyBtn.style.display = '';
      var html = '';
      if (tasks.length) {
        html += '<div class="section-title">Deleted tasks</div><div class="list">' +
          tasks.map(function (t) {
            return trashRow('task', t.id, t.title, t.due_at ? 'Due ' + fmtDate(t.due_at) : '');
          }).join('') + '</div>';
      }
      if (notes.length) {
        html += '<div class="section-title">Deleted notes</div><div class="list">' +
          notes.map(function (n) {
            return trashRow('note', n.id, n.title || 'Untitled', (n.content || '').slice(0, 80));
          }).join('') + '</div>';
      }
      list.innerHTML = html;
    });
  }

  document.getElementById('tlist').addEventListener('click', function (e) {
    var rs = e.target.closest('[data-restore]');
    if (rs) {
      var parts = rs.getAttribute('data-restore').split(':');
      api('/' + parts[0] + 's/' + encodeURIComponent(parts[1]) + '/restore', { method: 'POST' })
        .then(function () { toast('Restored.', 'ok'); load(); })
        .catch(function (err) { toast(err.message, 'err'); });
      return;
    }
    var pm = e.target.closest('[data-perm]');
    if (pm) {
      if (!confirm('Delete forever? This cannot be undone.')) return;
      var p2 = pm.getAttribute('data-perm').split(':');
      api('/' + p2[0] + 's/' + encodeURIComponent(p2[1]) + '?permanent=true', { method: 'DELETE' })
        .then(function () { toast('Deleted forever.', 'ok'); load(); })
        .catch(function (err) { toast(err.message, 'err'); });
    }
  });

  document.getElementById('emptytrash').addEventListener('click', function () {
    if (!confirm('Delete everything in trash forever? This cannot be undone.')) return;
    Promise.all([api('/tasks?trashed=true'), api('/notes?trashed=true')]).then(function (res) {
      var dels = [];
      (res[0].tasks || []).forEach(function (t) {
        dels.push(api('/tasks/' + encodeURIComponent(t.id) + '?permanent=true', { method: 'DELETE' }));
      });
      (res[1].notes || []).forEach(function (n) {
        dels.push(api('/notes/' + encodeURIComponent(n.id) + '?permanent=true', { method: 'DELETE' }));
      });
      return Promise.all(dels);
    }).then(function () { toast('Trash emptied.', 'ok'); load(); })
      .catch(function (err) { toast(err.message, 'err'); });
  });

  return load();
}

function NoteEditorView(id) {
  var isNew = !id;
  mount(shell('notes',
    '<div class="page-head"><h2>' + (isNew ? 'New' : 'Edit') + ' <span class="grad-text">note</span></h2></div>' +
    '<div class="card">' + skel(5, 'skel-line') + '</div>'));

  var boot = isNew
    ? loadLabels().then(function () { return { note: null, items: [] }; })
    : Promise.all([
        api('/notes/' + encodeURIComponent(id)),
        api('/notes/' + encodeURIComponent(id) + '/items'),
        loadLabels()
      ]).then(function (res) { return { note: res[0].note, items: res[1].items || [] }; });

  return boot.then(function (ctx) { renderForm(ctx.note, ctx.items); }).catch(function (e) {
    if (e.status === 404) {
      mount(shell('notes', emptyHtml(ICONS.search, 'Note not found',
        'This note may have been deleted.', '<a class="btn btn-ghost btn-sm" href="#/notes">Back to notes</a>')));
      return;
    }
    throw e;
  });

  function renderForm(n, items) {
    n = n || { title: '', content: '', color: '#FFF7AE', pinned: false, archived: false };
    var color = NOTE_COLORS.indexOf(n.color) !== -1 ? n.color : NOTE_COLORS[0];
    var selLabels = {};
    (n.labels || []).forEach(function (l) { selLabels[l.id] = true; });
    var clItems = (items || []).slice();
    var mode = clItems.length ? 'list' : 'text';

    mount(shell('notes',
      '<div class="page-head"><h2>' + (isNew ? 'New' : 'Edit') + ' <span class="grad-text">note</span></h2></div>' +
      '<div class="card"><div class="form">' +
      '<div id="err"></div>' +
      '<div class="field"><label for="title">Title</label>' +
      '<input id="title" type="text" value="' + esc(n.title) + '" placeholder="Note title"></div>' +
      '<div class="chips flat" id="modechips">' +
      '<button type="button" class="chip' + (mode === 'text' ? ' active' : '') + '" data-v="text">Text</button>' +
      '<button type="button" class="chip' + (mode === 'list' ? ' active' : '') + '" data-v="list">Checklist</button></div>' +
      '<div id="textmode"' + (mode === 'list' ? ' hidden' : '') + '>' +
      '<div class="field"><label for="content">Content</label>' +
      '<textarea id="content" style="min-height:180px" placeholder="Write something…">' + esc(n.content) + '</textarea></div></div>' +
      '<div id="listmode"' + (mode === 'text' ? ' hidden' : '') + '>' +
      '<div class="checklist" id="cllist"></div>' +
      '<div class="add-row" style="margin-top:10px"><input id="newitem" type="text" placeholder="Add an item…">' +
      '<button type="button" class="btn btn-ghost btn-sm" id="additem">Add</button></div></div>' +
      '<div class="field"><label>Color</label><div class="swatches" id="sw">' +
      NOTE_COLORS.map(function (c) {
        return '<button type="button" class="swatch' + (c === color ? ' selected' : '') + '" data-c="' + c +
          '" style="background:' + c + '" aria-label="Color ' + c + '"></button>';
      }).join('') + '</div></div>' +
      '<div class="field"><label>Labels</label>' + labelChipsHtml(selLabels, 'ne') + '</div>' +
      (isNew ? '' :
        '<label class="toggle-row"><input type="checkbox" id="pinned"' + (n.pinned ? ' checked' : '') + '> Pinned</label>' +
        '<label class="toggle-row"><input type="checkbox" id="archived"' + (n.archived ? ' checked' : '') + '> Archived</label>') +
      '<div class="form-row"><a class="btn btn-ghost" href="#/notes" style="text-align:center">Cancel</a>' +
      '<button type="button" class="btn btn-primary" id="save">Save</button></div>' +
      (isNew ? '' : '<button type="button" class="btn btn-danger" id="del" style="width:100%">Delete note</button>') +
      '</div></div>'));

    function paintCl() {
      var box = document.getElementById('cllist');
      if (!box) return;
      box.innerHTML = clItems.length ? clItems.map(function (it, idx) {
        return '<div class="cl-row"><button type="button" class="checkbox" data-clt="' + idx + '"' +
          ' aria-pressed="' + !!it.checked + '" aria-label="Toggle item">' + ICONS.check + '</button>' +
          '<span class="cl-text' + (it.checked ? ' done' : '') + '">' + esc(it.text) + '</span>' +
          '<button type="button" class="icon-btn" data-cld="' + idx + '" aria-label="Delete item">' + ICONS.x + '</button></div>';
      }).join('') : '<p class="hint">No items yet — add your first below.</p>';
    }
    paintCl();

    bindSeg('modechips', function (v) {
      mode = v;
      document.getElementById('textmode').hidden = mode !== 'text';
      document.getElementById('listmode').hidden = mode !== 'list';
    });

    document.getElementById('cllist').addEventListener('click', function (e) {
      var tg = e.target.closest('[data-clt]');
      if (tg) {
        var idx = parseInt(tg.getAttribute('data-clt'), 10);
        var it = clItems[idx];
        var checked = !it.checked;
        if (isNew || !it.id) { it.checked = checked; paintCl(); return; }
        api('/notes/' + encodeURIComponent(id) + '/items/' + encodeURIComponent(it.id),
          { method: 'PUT', body: { checked: checked } })
          .then(function (d) { it.checked = d.item.checked; clCache[id] = clItems; paintCl(); })
          .catch(function (err) { toast(err.message, 'err'); });
        return;
      }
      var del = e.target.closest('[data-cld]');
      if (del) {
        var dIdx = parseInt(del.getAttribute('data-cld'), 10);
        var ditem = clItems[dIdx];
        if (isNew || !ditem.id) { clItems.splice(dIdx, 1); paintCl(); return; }
        api('/notes/' + encodeURIComponent(id) + '/items/' + encodeURIComponent(ditem.id), { method: 'DELETE' })
          .then(function () { clItems.splice(dIdx, 1); clCache[id] = clItems; paintCl(); })
          .catch(function (err) { toast(err.message, 'err'); });
      }
    });

    function addItem() {
      var inp = document.getElementById('newitem');
      var text = inp.value.trim();
      if (!text) return;
      if (isNew) { clItems.push({ text: text, checked: false }); inp.value = ''; paintCl(); return; }
      api('/notes/' + encodeURIComponent(id) + '/items', { method: 'POST', body: { text: text } })
        .then(function (d) { clItems.push(d.item); clCache[id] = clItems; inp.value = ''; paintCl(); })
        .catch(function (err) { toast(err.message, 'err'); });
    }
    document.getElementById('additem').addEventListener('click', addItem);
    document.getElementById('newitem').addEventListener('keydown', function (e) {
      if (e.key === 'Enter') addItem();
    });

    var picked = color;
    document.getElementById('sw').addEventListener('click', function (e) {
      var s = e.target.closest('[data-c]');
      if (!s) return;
      picked = s.getAttribute('data-c');
      document.querySelectorAll('#sw .swatch').forEach(function (el) {
        el.classList.toggle('selected', el.getAttribute('data-c') === picked);
      });
    });
    bindLabelChips('ne');

    document.getElementById('save').addEventListener('click', function () {
      var btn = this;
      btn.disabled = true;
      var body = {
        title: document.getElementById('title').value,
        content: mode === 'text' ? document.getElementById('content').value : (n.content || ''),
        color: picked,
        label_ids: collectLabelIds('ne')
      };
      if (!isNew) {
        body.pinned = document.getElementById('pinned').checked;
        body.archived = document.getElementById('archived').checked;
      }
      var req = isNew
        ? api('/notes', { method: 'POST', body: body })
        : api('/notes/' + encodeURIComponent(id), { method: 'PUT', body: body });
      req.then(function (data) {
        var nid = data.note.id;
        var pending = (mode === 'list') ? clItems.filter(function (it) { return !it.id; }) : [];
        var finish = function () {
          clCache[nid] = clItems;
          location.hash = '#/notes/' + encodeURIComponent(nid);
        };
        if (!pending.length) { finish(); return; }
        var chain = Promise.resolve();
        pending.forEach(function (it) {
          chain = chain.then(function () {
            return api('/notes/' + encodeURIComponent(nid) + '/items', { method: 'POST', body: { text: it.text } });
          });
        });
        chain.then(finish).catch(function (e) {
          document.getElementById('err').innerHTML = errBanner(e.message);
          btn.disabled = false;
        });
      }).catch(function (e) {
        document.getElementById('err').innerHTML = errBanner(e.message);
        btn.disabled = false;
      });
    });

    if (!isNew) {
      document.getElementById('del').addEventListener('click', function () {
        if (!confirm('Delete this note?')) return;
        api('/notes/' + encodeURIComponent(id), { method: 'DELETE' })
          .then(function () { location.hash = '#/notes'; })
          .catch(function (e) { toast(e.message, 'err'); });
      });
    }
  }
}

/* ---------------- schedule ---------------- */

var schedDays = 7;

function ScheduleView() {
  mount(shell('schedule',
    '<div class="page-head"><h2><span class="grad-text">Schedule</span></h2></div>' +
    '<div class="chips" id="days">' +
    [7, 14, 30].map(function (d) {
      return '<button class="chip' + (d === schedDays ? ' active' : '') + '" data-d="' + d + '">Next ' + d + ' days</button>';
    }).join('') + '</div>' +
    '<div id="list">' + skel(3, 'skel-row') + '</div>'));

  function todayKey() {
    var d = new Date();
    return d.getFullYear() + '-' + pad(d.getMonth() + 1) + '-' + pad(d.getDate());
  }

  function load() {
    return api('/schedule/upcoming?days=' + schedDays).then(function (data) {
      var items = data.items || [];
      var list = document.getElementById('list');
      if (!items.length) {
        list.innerHTML = emptyHtml(ICONS.schedule, 'Nothing scheduled',
          'Tasks with due dates or reminders in the next ' + schedDays + ' days will appear here, grouped by day.');
        return;
      }
      var groups = {};
      items.forEach(function (it) {
        var key = (it.due_at || it.reminder_at || '').slice(0, 10);
        (groups[key] = groups[key] || []).push(it);
      });
      var tk = todayKey();
      list.innerHTML = '<div class="timeline">' + Object.keys(groups).sort().map(function (key) {
        var dt = new Date(key + 'T00:00:00');
        var rows = groups[key].map(function (it) {
          var when = it.due_at || it.reminder_at;
          var overdue = isOverdue(when);
          return '<div class="task-row" data-id="' + esc(it.id) + '">' +
            '<div class="task-main"><div class="task-title">' + esc(it.title) + '</div>' +
            '<div class="task-meta">' + priorityPill(it.priority) +
            '<span class="due' + (overdue ? ' overdue' : '') + '">' + ICONS.clock + esc(fmtDate(when)) + '</span>' +
            '</div></div></div>';
        }).join('');
        return '<div class="tl-day' + (key === tk ? ' today' : '') + '">' +
          '<div class="tl-badge"><span class="d">' + dt.getDate() + '</span>' +
          '<span class="m">' + esc(dt.toLocaleDateString(undefined, { month: 'short' })) + '</span></div>' +
          '<div class="tl-body"><div class="tl-date">' + esc(key === tk ? 'Today' : fmtDay(key + 'T00:00:00')) + '</div>' +
          '<div class="list">' + rows + '</div></div></div>';
      }).join('') + '</div>';
    });
  }

  document.getElementById('days').addEventListener('click', function (e) {
    var b = e.target.closest('[data-d]');
    if (!b) return;
    schedDays = parseInt(b.getAttribute('data-d'), 10);
    document.querySelectorAll('#days .chip').forEach(function (c) {
      c.classList.toggle('active', parseInt(c.getAttribute('data-d'), 10) === schedDays);
    });
    load().catch(function (err) { toast(err.message, 'err'); });
  });

  document.getElementById('list').addEventListener('click', function (e) {
    var row = e.target.closest('.task-row');
    if (row) location.hash = '#/tasks/' + encodeURIComponent(row.getAttribute('data-id'));
  });

  return load();
}

/* ---------------- profile ---------------- */

function ProfileView() {
  mount(shell('profile',
    '<div class="page-head"><h2><span class="grad-text">Profile</span></h2></div>' +
    '<div class="card profile-card">' + skel(3, 'skel-line') + '</div>'));

  return api('/auth/me').then(function (data) {
    var u = data.user;
    var initial = (u.name || u.email || '?').charAt(0).toUpperCase();
    mount(shell('profile',
      '<div class="page-head"><h2><span class="grad-text">Profile</span></h2></div>' +
      '<div class="card profile-card">' +
      '<div class="avatar">' + esc(initial) + '</div>' +
      '<h2>' + esc(u.name || '—') + '</h2>' +
      '<div class="email">' + esc(u.email || '') + '</div>' +
      '<div class="task-meta" style="justify-content:center;margin-bottom:20px">' +
      '<span class="pill pill-neutral">Member since ' + esc(fmtDay((u.created_at || '').slice(0, 10) + 'T00:00:00')) + '</span></div>' +
      '<button class="btn btn-danger" id="logout" style="width:100%">Log out</button>' +
      '</div>'));

    document.getElementById('logout').addEventListener('click', function () {
      api('/auth/logout', { method: 'POST' }).catch(function () {});
      localStorage.removeItem(TOKEN_KEY);
      location.hash = '#/login';
    });
  });
}

/* ---------------- boot ---------------- */

initGlobalSearch();
if (!location.hash) location.hash = getToken() ? '#/tasks' : '#/login';
router();
