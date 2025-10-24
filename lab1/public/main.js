(() => {
  const allowedX = new Set([-5, -4, -3, -2, -1, 0, 1, 2, 3].map(String));
  const allowedR = new Set([1, 2, 3, 4, 5].map(String));
  const historyLimit = 100;
  const fixedExtent = 6; // covers slightly more than allowed range (-5..5)

  let history = [];
  let currentPoint = null;

  const els = {
    form: document.getElementById('hit-form'),
    xHidden: document.getElementById('x'),
    xButtons: document.getElementById('x-buttons'),
    y: document.getElementById('y'),
    r: document.getElementById('r'),
    submit: document.getElementById('submit'),
    random: document.getElementById('random'),
    errors: document.getElementById('errors'),
    canvas: document.getElementById('plot-canvas'),
    tableBody: document.querySelector('#results-table tbody')
  };

  const xButtons = [...els.xButtons.querySelectorAll('button')];

  els.xButtons.addEventListener('click', (e) => {
    if (!(e.target instanceof HTMLButtonElement) || !e.target.dataset.x) return;
    const x = e.target.dataset.x;
    if (!allowedX.has(x)) return;
    selectX(x);
  });

  els.random.addEventListener('click', () => {
    const xs = Array.from(allowedX).map(Number);
    const x = xs[Math.floor(Math.random() * xs.length)];
    const y = Math.random() * 6 - 3; // [-3,3)
    selectX(String(x));
    els.y.value = y.toFixed(3);
    els.errors.textContent = '';
    els.form.requestSubmit();
  });

  els.form.addEventListener('submit', async (e) => {
    e.preventDefault();
    els.errors.textContent = '';
    const v = validate();
    if (!v.ok) {
      els.errors.textContent = v.errs.join(' ');
      return;
    }
    const params = new URLSearchParams({
      x: els.xHidden.value,
      y: els.y.value.replace(',', '.'),
      r: els.r.value
    });
    const url = `/api/check?${params.toString()}`;
    try {
      const res = await fetch(url, {
        method: 'GET',
        credentials: 'include',
        headers: { Accept: 'application/json' }
      });
      if (!res.ok) {
        const message = await responseMessage(res);
        els.errors.textContent = message || 'Ошибка запроса.';
        return;
      }
      const json = await res.json();
      applyServerHistory(json);
    } catch (err) {
      els.errors.textContent = 'Сеть недоступна или сервер не запущен.';
    }
  });

  els.r.addEventListener('change', draw);
  window.addEventListener('resize', draw);

  restoreHistory().finally(draw);

  function selectX(value) {
    els.xHidden.value = value ?? '';
    for (const btn of xButtons) {
      const isSelected = btn.dataset.x === value;
      btn.classList.toggle('selected', isSelected);
      btn.setAttribute('aria-pressed', isSelected ? 'true' : 'false');
    }
  }

  function validate() {
    const errs = [];
    const x = els.xHidden.value;
    const yStr = els.y.value;
    const r = els.r.value;

    if (!allowedX.has(x)) errs.push('Выберите X из диапазона от -5 до 3.');
    const y = parseNumber(yStr);
    if (y === null) errs.push('Введите число для Y (можно с точкой или запятой).');
    else if (y < -3 || y > 3) errs.push('Y должен быть в диапазоне от -3 до 3 включительно.');
    if (!allowedR.has(r)) errs.push('Выберите значение R от 1 до 5.');
    return { ok: errs.length === 0, errs };
  }

  function parseNumber(str) {
    if (typeof str !== 'string') return null;
    const s = str.trim().replace(',', '.');
    if (!/^[-+]?\d*(?:[.]\d+)?$/.test(s)) return null;
    const n = Number(s);
    return Number.isFinite(n) ? n : null;
  }

  function applyServerHistory(json) {
    const hist = Array.isArray(json.history) ? json.history : [];
    const fallbackTime = json.nowIso ?? new Date().toISOString();
    const parsedRows = hist
      .map((row) => {
        const parsed = typeof row === 'string' ? safeJsonParse(row) : row;
        const rawY = parsed?.y ?? json.y;
        const rawYStr = typeof rawY === 'string' ? rawY : (rawY != null ? String(rawY) : '');
        const yNumeric = parseNumber(rawYStr);
        return {
          x: Number(parsed?.x ?? json.x),
          y: yNumeric,
          yDisplay: rawYStr,
          r: Number(parsed?.r ?? json.r),
          hit: Boolean(parsed?.hit ?? json.hit),
          timestamp: parsed?.timestamp ?? fallbackTime,
          durationMs: Number(parsed?.durationMs ?? json.durationMs ?? 0)
        };
      })
      .filter((p) => Number.isFinite(p.x) && Number.isFinite(p.y) && Number.isFinite(p.r));

    history = parsedRows.slice(0, historyLimit);
    currentPoint = history.length > 0 ? history[0] : null;
    renderTable(history);
    draw();
  }

  function renderTable(rows) {
    const tbody = els.tableBody;
    if (!tbody) return;
    tbody.innerHTML = '';
    for (const r of rows) {
      const tr = document.createElement('tr');
      const cells = [
        r.x,
        formatY(r.yDisplay ?? r.y),
        r.r,
        r.hit ? 'Да' : 'Нет',
        formatTime(r.timestamp),
        formatDuration(r.durationMs)
      ];
      cells.forEach((value, idx) => {
        const td = document.createElement('td');
        td.textContent = value;
        if (idx === 3) td.className = r.hit ? 'ok' : 'fail';
        tr.appendChild(td);
      });
      tbody.appendChild(tr);
    }
  }

  function draw() {
    const c = els.canvas;
    if (!c) return;
    const ratio = window.devicePixelRatio || 1;
    const cssWidth = c.clientWidth || 640;
    const cssHeight = c.clientHeight || 640;
    const targetW = Math.round(cssWidth * ratio);
    const targetH = Math.round(cssHeight * ratio);
    if (c.width !== targetW || c.height !== targetH) {
      c.width = targetW;
      c.height = targetH;
    }

    const ctx = c.getContext('2d');
    ctx.save();
    try {
      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.clearRect(0, 0, c.width, c.height);
      ctx.scale(ratio, ratio);
      ctx.imageSmoothingEnabled = true;

      const pad = 24;
      const w = cssWidth;
      const h = cssHeight;
      const currentR = Number(els.r.value || '1');
      const scale = (Math.min(w, h) / 2 - pad) / fixedExtent;
      const cx = w / 2;
      const cy = h / 2;
      const lineWidth = 1 / ratio;

      const styles = getComputedStyle(document.documentElement);
      const axisColor = styles.getPropertyValue('--axis').trim() || '#4a3f40';
      const tickColor = styles.getPropertyValue('--tick').trim() || '#c7aeb3';
      const labelColor = styles.getPropertyValue('--muted').trim() || '#6f5d5f';
      const accent = styles.getPropertyValue('--accent').trim() || '#d16b8f';
      const hitColor = styles.getPropertyValue('--hit').trim() || '#3a7f6f';
      const missColor = styles.getPropertyValue('--miss').trim() || '#c94c40';

      ctx.strokeStyle = axisColor;
      ctx.lineWidth = lineWidth;
      ctx.beginPath();
      ctx.moveTo(pad / 2, cy);
      ctx.lineTo(w - pad / 2, cy);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(cx, pad / 2);
      ctx.lineTo(cx, h - pad / 2);
      ctx.stroke();

      const ticks = [-fixedExtent, -fixedExtent / 2, 0, fixedExtent / 2, fixedExtent];
      ctx.strokeStyle = tickColor;
      ctx.fillStyle = labelColor;
      ctx.font = '12px system-ui, sans-serif';
      for (const t of ticks) {
        const label = formatTick(t);
        const px = cx + t * scale;
        const py = cy - t * scale;

        ctx.beginPath();
        ctx.moveTo(px, cy - 4);
        ctx.lineTo(px, cy + 4);
        ctx.stroke();
        ctx.textAlign = 'center';
        ctx.textBaseline = 'top';
        ctx.fillText(label, px, cy + 8);

        ctx.beginPath();
        ctx.moveTo(cx - 4, py);
        ctx.lineTo(cx + 4, py);
        ctx.stroke();
        ctx.textAlign = 'left';
        ctx.textBaseline = 'middle';
        ctx.fillText(label, cx + 8, py);
      }

      ctx.fillStyle = accent;
      ctx.globalAlpha = 0.28;
      ctx.beginPath();
      rectPath(ctx, scale, cx, cy, 0, 0, currentR, currentR / 2);
      ctx.fill();

      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.arc(cx, cy, (currentR / 2) * scale, Math.PI, 1.5 * Math.PI, false);
      ctx.closePath();
      ctx.fill();

      ctx.beginPath();
      const triA = toPx(scale, cx, cy, -currentR, 0);
      const triB = toPx(scale, cx, cy, 0, 0);
      const triC = toPx(scale, cx, cy, 0, -currentR / 2);
      ctx.moveTo(triA.x, triA.y);
      ctx.lineTo(triB.x, triB.y);
      ctx.lineTo(triC.x, triC.y);
      ctx.closePath();
      ctx.fill();
      ctx.globalAlpha = 1;

      if (!currentPoint) return;

      const pointPx = toPx(scale, cx, cy, currentPoint.x, currentPoint.y);
      const alpha = Number(currentPoint.r) === currentR ? 1 : 0.35;
      ctx.beginPath();
      ctx.arc(pointPx.x, pointPx.y, 4, 0, Math.PI * 2);
      ctx.fillStyle = currentPoint.hit ? hitColor : missColor;
      ctx.globalAlpha = alpha;
      ctx.fill();
      ctx.lineWidth = 1.2;
      ctx.strokeStyle = axisColor;
      ctx.stroke();
    } finally {
      ctx.globalAlpha = 1;
      ctx.restore();
    }
  }

  function rectPath(ctx, scale, cx, cy, x0, y0, x1, y1) {
    const a = toPx(scale, cx, cy, x0, y0);
    const b = toPx(scale, cx, cy, x1, y1);
    ctx.moveTo(a.x, a.y);
    ctx.lineTo(b.x, a.y);
    ctx.lineTo(b.x, b.y);
    ctx.lineTo(a.x, b.y);
    ctx.closePath();
  }

  function toPx(scale, cx, cy, x, y) {
    return { x: cx + x * scale, y: cy - y * scale };
  }

  function formatTick(value) {
    return Number.isInteger(value) ? value.toString() : value.toFixed(1);
  }

  function formatY(y) {
    if (typeof y === 'string' && y.trim() !== '') return y;
    if (typeof y === 'number' && Number.isFinite(y)) return y.toString();
    return '';
  }

  function formatTime(s) {
    try {
      return new Date(s).toLocaleString();
    } catch {
      return String(s);
    }
  }

  function formatDuration(ms) {
    const value = Number(ms);
    if (!Number.isFinite(value)) return '0';
    return value.toFixed(3);
  }

  async function responseMessage(res) {
    let body = '';
    try {
      body = await res.text();
    } catch {
      return '';
    }
    const trimmed = body.trim();
    const contentType = res.headers.get('content-type') ?? '';
    if (contentType.includes('application/json')) {
      try {
        const data = JSON.parse(body);
        if (data && Array.isArray(data.errors) && data.errors.length > 0) {
          return data.errors.join(' ');
        }
      } catch {
        // fallback to the raw body below
      }
    }
    return trimmed;
  }

  async function restoreHistory() {
    try {
      const res = await fetch('/api/check', {
        method: 'GET',
        credentials: 'include',
        headers: { Accept: 'application/json' }
      });
      if (!res.ok) return;
      const json = await res.json();
      if (json && Array.isArray(json.history)) applyServerHistory(json);
    } catch (err) {
      console.error('Failed to restore history', err);
    }
  }

  function safeJsonParse(str) {
    try {
      return JSON.parse(str);
    } catch {
      return null;
    }
  }
})();
