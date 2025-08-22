async function apiFetch(url, options={}) {
    let res = await fetch(url, {credentials: 'same-origin', ...options});
    if (res.status === 401) {
        // 尝试刷新
        const r = await fetch('/api/auth/refresh', {method:'POST', credentials:'same-origin'});
        if (r.ok) {
            // 再试一次
            res = await fetch(url, {credentials: 'same-origin', ...options});
        } else {
            location.href = '/login?redirect=' + encodeURIComponent(location.pathname);
        }
    }
    return res;
}
