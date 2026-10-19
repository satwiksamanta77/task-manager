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
  ok: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="9"/><path d="M8.5 12.5l2.5 2.5 4.5-5.5"/></svg>'
};

var NAV = [
  { id: 'tasks', label: 'Tasks', href: '#/tasks' },
  { id: 'notes', label: 'Notes', href: '#/notes' },
  { id: 'schedule', label: 'Schedule', href: '#/schedule' },
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
    '<div class="main"><main class="container">' + content + '</main></div>' +
    '<nav class="bottomnav">' + navHtml(active) + '</nav>' +
    '</div>';
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
    (due ? '<span class="due' + (overdue ? ' overdue' : '') + '">' + ICONS.cal + esc(due) + '</span>' : '') +
    '</div></div></div>';
}

function TasksView() {
  mount(shell('tasks',
    '<div class="page-head"><h2>My <span class="grad-text">Tasks</span></h2></div>' +
    '<div class="chips" id="filters">' +
    TASK_FILTERS.map(function (f) {
      return '<button class="chip' + (f.id === taskFilter ? ' active' : '') + '" data-f="' + f.id + '">' + f.label + '</button>';
    }).join('') + '</div>' +
    '<div class="list" id="list">' + skel(4, 'skel-row') + '</div>' +
    '<button class="fab" id="fab" aria-label="Add task">' + ICONS.plus + '</button>'));

  function load() {
    var q = taskFilter === 'all' ? '' : '?status=' + taskFilter;
    return api('/tasks' + q).then(function (data) {
      var list = document.getElementById('list');
      if (!data.tasks.length) {
        list.innerHTML = emptyHtml(ICONS.tasks, 'All clear',
          taskFilter === 'done'
            ? 'Nothing completed yet. Finish a task and it will show up here.'
            : 'No tasks here. Tap + to capture your first one — due dates and subtasks included.');
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
      }).then(load).catch(function (err) { toast(err.message, 'err'); load(); });
      return;
    }
    var row = e.target.closest('.task-row');
    if (row) location.hash = '#/tasks/' + encodeURIComponent(row.getAttribute('data-id'));
  });

  document.getElementById('fab').addEventListener('click', function () {
    location.hash = '#/tasks/new';
  });

  return load();
}

function TaskNewView() {
  mount(shell('tasks',
    '<div class="page-head"><h2>New <span class="grad-text">Task</span></h2></div>' +
    '<div class="card"><div class="form">' +
    '<div id="err"></div>' +
    '<div class="field"><label for="title">Title</label>' +
    '<input id="title" type="text" placeholder="What needs doing?"></div>' +
    '<div class="field"><label for="desc">Description</label>' +
    '<textarea id="desc" placeholder="Details (optional)"></textarea></div>' +
    '<div class="field"><label for="priority">Priority</label>' +
    '<select id="priority"><option value="low">Low</option>' +
    '<option value="medium" selected>Medium</option><option value="high">High</option></select></div>' +
    '<div class="field"><label for="due">Due date</label>' +
    '<input id="due" type="datetime-local"></div>' +
    '<div class="field"><label for="rem">Reminder</label>' +
    '<input id="rem" type="datetime-local"></div>' +
    '<div class="form-row"><a class="btn btn-ghost" href="#/tasks" style="text-align:center">Cancel</a>' +
    '<button class="btn btn-primary" id="save">Create task</button></div>' +
    '</div></div>'));

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
  return Promise.resolve();
}

function TaskDetailView(id) {
  mount(shell('tasks', '<div class="page-head"><h2>Task <span class="grad-text">details</span></h2></div>' +
    '<div class="card">' + skel(5, 'skel-line') + '</div>'));

  return api('/tasks/' + encodeURIComponent(id)).then(function (data) {
    var t = data.task;
    var subs = t.subtasks || [];
    mount(shell('tasks',
      '<div class="page-head"><h2>Edit <span class="grad-text">task</span></h2></div>' +
      '<div class="card"><div class="form">' +
      '<div id="err"></div>' +
      '<div class="field"><label for="title">Title</label>' +
      '<input id="title" type="text" value="' + esc(t.title) + '"></div>' +
      '<div class="field"><label for="desc">Description</label>' +
      '<textarea id="desc">' + esc(t.description || '') + '</textarea></div>' +
      '<div class="field"><label for="status">Status</label><select id="status">' +
      '<option value="todo"' + (t.status === 'todo' ? ' selected' : '') + '>To do</option>' +
      '<option value="done"' + (t.status === 'done' ? ' selected' : '') + '>Done</option></select></div>' +
      '<div class="field"><label for="priority">Priority</label><select id="priority">' +
      ['low', 'medium', 'high'].map(function (p) {
        return '<option value="' + p + '"' + (t.priority === p ? ' selected' : '') + '>' +
          p.charAt(0).toUpperCase() + p.slice(1) + '</option>';
      }).join('') + '</select></div>' +
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
          due_at: fromLocalInput(document.getElementById('due').value),
          reminder_at: fromLocalInput(document.getElementById('rem').value)
        }
      }).then(function () { toast('Saved.', 'ok'); reload(); })
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
    '</article>';
}

function NotesView() {
  mount(shell('notes',
    '<div class="page-head"><h2>My <span class="grad-text">Notes</span></h2></div>' +
    '<div class="searchbar">' + ICONS.search + '<input id="search" type="search" placeholder="Search notes…" value="' + esc(noteSearch) + '" aria-label="Search notes"></div>' +
    '<div class="chips">' +
    '<button class="chip' + (!showArchived ? ' active' : '') + '" data-arch="0">Notes</button>' +
    '<button class="chip' + (showArchived ? ' active' : '') + '" data-arch="1">Archived</button>' +
    '</div>' +
    '<div class="notes-masonry" id="grid">' + skel(6, 'skel-note') + '</div>' +
    '<button class="fab" id="fab" aria-label="Add note">' + ICONS.plus + '</button>'));

  var cache = [];

  function paint() {
    var grid = document.getElementById('grid');
    var q = noteSearch.trim().toLowerCase();
    var items = cache.filter(function (n) {
      return !q || (n.title + ' ' + n.content).toLowerCase().indexOf(q) !== -1;
    });
    grid.innerHTML = items.length ? items.map(noteCard).join('') :
      emptyHtml(ICONS.notes, q ? 'No matches' : 'No notes yet',
        q ? 'Try a different search term.'
          : 'Jot down ideas, lists and reminders. They sync instantly across your devices.');
  }

  function load() {
    return api('/notes?archived=' + (showArchived ? 'true' : 'false')).then(function (data) {
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

  return load();
}

function NoteEditorView(id) {
  var isNew = !id;
  mount(shell('notes',
    '<div class="page-head"><h2>' + (isNew ? 'New' : 'Edit') + ' <span class="grad-text">note</span></h2></div>' +
    '<div class="card"><div class="empty">Loading…</div></div>'));

  function renderForm(n) {
    n = n || { title: '', content: '', color: '#FFF7AE', pinned: false, archived: false };
    var color = NOTE_COLORS.indexOf(n.color) !== -1 ? n.color : NOTE_COLORS[0];
    mount(shell('notes',
      '<div class="page-head"><h2>' + (isNew ? 'New' : 'Edit') + ' <span class="grad-text">note</span></h2></div>' +
      '<div class="card"><div class="form">' +
      '<div id="err"></div>' +
      '<div class="field"><label for="title">Title</label>' +
      '<input id="title" type="text" value="' + esc(n.title) + '" placeholder="Note title"></div>' +
      '<div class="field"><label for="content">Content</label>' +
      '<textarea id="content" style="min-height:180px" placeholder="Write something…">' + esc(n.content) + '</textarea></div>' +
      '<div class="field"><label>Color</label><div class="swatches" id="sw">' +
      NOTE_COLORS.map(function (c) {
        return '<button type="button" class="swatch' + (c === color ? ' selected' : '') + '" data-c="' + c +
          '" style="background:' + c + '" aria-label="Color ' + c + '"></button>';
      }).join('') + '</div></div>' +
      (isNew ? '' :
        '<label class="toggle-row"><input type="checkbox" id="pinned"' + (n.pinned ? ' checked' : '') + '> Pinned</label>' +
        '<label class="toggle-row"><input type="checkbox" id="archived"' + (n.archived ? ' checked' : '') + '> Archived</label>') +
      '<div class="form-row"><a class="btn btn-ghost" href="#/notes" style="text-align:center">Cancel</a>' +
      '<button class="btn btn-primary" id="save">Save</button></div>' +
      (isNew ? '' : '<button class="btn btn-danger" id="del" style="width:100%">Delete note</button>') +
      '</div></div>'));

    var picked = color;
    document.getElementById('sw').addEventListener('click', function (e) {
      var s = e.target.closest('[data-c]');
      if (!s) return;
      picked = s.getAttribute('data-c');
      document.querySelectorAll('#sw .swatch').forEach(function (el) {
        el.classList.toggle('selected', el.getAttribute('data-c') === picked);
      });
    });

    document.getElementById('save').addEventListener('click', function () {
      var btn = this;
      btn.disabled = true;
      var body = {
        title: document.getElementById('title').value,
        content: document.getElementById('content').value,
        color: picked
      };
      if (!isNew) {
        body.pinned = document.getElementById('pinned').checked;
        body.archived = document.getElementById('archived').checked;
      }
      var req = isNew
        ? api('/notes', { method: 'POST', body: body })
        : api('/notes/' + encodeURIComponent(id), { method: 'PUT', body: body });
      req.then(function (data) {
        location.hash = '#/notes/' + encodeURIComponent(data.note.id);
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

  if (isNew) { renderForm(null); return Promise.resolve(); }
  return api('/notes/' + encodeURIComponent(id)).then(function (data) {
    renderForm(data.note);
  }).catch(function (e) {
    if (e.status === 404) {
      mount(shell('notes', emptyHtml(ICONS.search, 'Note not found',
        'This note may have been deleted.', '<a class="btn btn-ghost btn-sm" href="#/notes">Back to notes</a>')));
      return;
    }
    throw e;
  });
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

if (!location.hash) location.hash = getToken() ? '#/tasks' : '#/login';
router();
