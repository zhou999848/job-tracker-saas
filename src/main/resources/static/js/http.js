// static/js/http.js
const CSRF_TOKEN  = document.querySelector('meta[name="_csrf"]')?.content;
const CSRF_HEADER = document.querySelector('meta[name="_csrf_header"]')?.content;

async function apiFetch(url, options = {}) {
    const opt = { credentials: 'same-origin', headers: { ...(options.headers||{}) }, ...options };
    if (CSRF_TOKEN && CSRF_HEADER && !opt.headers[CSRF_HEADER]) opt.headers[CSRF_HEADER] = CSRF_TOKEN;
    const res = await fetch(url, opt);
    if (res.status === 401) {
        const back = encodeURIComponent(location.pathname + location.search);
        location.href = '/login?redirect=' + back;
        throw new Error('Unauthorized');
    }
    if (!res.ok) {
        let msg = 'Error ' + res.status;
        try { const j = await res.json(); msg = j.message || msg; } catch {}
        throw new Error(msg);
    }
    const ct = res.headers.get('content-type') || '';
    return ct.includes('application/json') ? res.json() : res.text();
}
window.apiFetch = apiFetch; // 挂到全局，页面里可直接用 apiFetch(...)
