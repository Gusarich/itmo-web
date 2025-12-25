(() => {
  const form = document.getElementById('hit-form');
  if (!form) return;

  const xInput = document.getElementById('x-input');
  const rSelect = document.getElementById('r-select');
  const yClickInput = document.getElementById('y-click');
  const errorBox = document.getElementById('errors');
  const randomBtn = document.getElementById('random-btn');
  const canvas = document.getElementById('plot-canvas');
  const checkboxNodes = Array.from(form.querySelectorAll('input[name="y"]'));

  const allowedY = checkboxNodes.map((node) => node.value);
  const historyData = document.getElementById('history-data');
  let history = [];
  if (historyData) {
    try {
      history = JSON.parse(historyData.textContent.trim() || '[]');
    } catch (err) {
      console.warn('Cannot parse history JSON', err);
    }
  }

  const fixedExtent = 6;

  // Client-side validation on form submit
  form.addEventListener('submit', (event) => {
    const validation = validate();
    if (!validation.ok) {
      event.preventDefault();
      renderErrors(validation.errors);
    } else {
      renderErrors([]);
    }
  });

  // Random point button
  randomBtn?.addEventListener('click', () => {
    const randomX = (Math.random() * 6 - 3).toFixed(3);
    const randomY = allowedY[Math.floor(Math.random() * allowedY.length)];
    const randomR = String(1 + Math.floor(Math.random() * 5));

    xInput.value = randomX;
    checkboxNodes.forEach((node) => {
      node.checked = node.value === randomY;
    });
    rSelect.value = randomR;
    yClickInput.value = '';
    renderErrors([]);
    form.requestSubmit();
  });

  // Clear yClick when checkboxes change
  checkboxNodes.forEach((node) => {
    node.addEventListener('change', () => {
      yClickInput.value = '';
    });
  });

  // Clear yClick when X changes
  xInput?.addEventListener('input', () => {
    yClickInput.value = '';
  });

  // Interactive canvas click handler
  canvas?.addEventListener('click', (event) => {
    // Task requirement: show message if R is not set
    if (!rSelect.value) {
      renderErrors(['Невозможно определить координаты точки: сначала выберите R.']);
      return;
    }

    const rect = canvas.getBoundingClientRect();
    const cssX = event.clientX - rect.left;
    const cssY = event.clientY - rect.top;
    const x = cssToLogicalX(cssX, canvas);
    const y = cssToLogicalY(cssY, canvas);

    xInput.value = x.toFixed(3);
    checkboxNodes.forEach((node) => { node.checked = false; });
    yClickInput.value = y.toFixed(3);
    renderErrors([]);
    form.requestSubmit();
  });

  // Redraw on R change and window resize
  rSelect?.addEventListener('change', () => draw());
  window.addEventListener('resize', () => draw());
  draw();

  /**
   * Client-side validation matching server-side rules
   */
  function validate() {
    const errors = [];

    // X validation
    const xVal = (xInput.value || '').trim().replace(',', '.');
    if (!xVal) {
      errors.push('Введите значение X.');
    } else if (!/^[-+]?\d*(?:[.]\d+)?$/.test(xVal)) {
      errors.push('X должен быть числом.');
    } else {
      const num = Number(xVal);
      if (!Number.isFinite(num) || num < -3 || num > 3) {
        errors.push('X должен быть в диапазоне от -3 до 3.');
      }
    }

    // Y validation
    const checked = checkboxNodes.filter((node) => node.checked);
    const hasClickY = !!(yClickInput.value && yClickInput.value.trim() !== '');
    if (checked.length === 0 && !hasClickY) {
      errors.push('Выберите хотя бы одно значение Y.');
    }

    // R validation
    const rVal = rSelect.value;
    if (!rVal) {
      errors.push('Выберите R.');
    }

    return { ok: errors.length === 0, errors };
  }

  /**
   * Render error messages in the error box
   */
  function renderErrors(errs) {
    if (!errorBox) return;
    if (!errs || errs.length === 0) {
      errorBox.innerHTML = '';
      return;
    }
    const list = document.createElement('ul');
    errs.forEach((err) => {
      const li = document.createElement('li');
      li.textContent = err;
      list.appendChild(li);
    });
    errorBox.innerHTML = '';
    errorBox.appendChild(list);
  }

  /**
   * Convert CSS X coordinate to logical X
   */
  function cssToLogicalX(cssX, canvas) {
    const cssWidth = canvas.clientWidth;
    const pad = 24;
    const w = cssWidth;
    const cx = w / 2;
    const scale = (Math.min(w, canvas.clientHeight) / 2 - pad) / fixedExtent;
    return (cssX - cx) / scale;
  }

  /**
   * Convert CSS Y coordinate to logical Y
   */
  function cssToLogicalY(cssY, canvas) {
    const cssHeight = canvas.clientHeight;
    const pad = 24;
    const h = cssHeight;
    const cy = h / 2;
    const scale = (Math.min(canvas.clientWidth, h) / 2 - pad) / fixedExtent;
    return (cy - cssY) / scale;
  }

  /**
   * Draw the coordinate plane, area, and history points
   */
  function draw() {
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const ratio = window.devicePixelRatio || 1;
    const cssWidth = canvas.clientWidth || 640;
    const cssHeight = canvas.clientHeight || 640;
    const targetW = Math.round(cssWidth * ratio);
    const targetH = Math.round(cssHeight * ratio);
    if (canvas.width !== targetW || canvas.height !== targetH) {
      canvas.width = targetW;
      canvas.height = targetH;
    }

    ctx.save();
    try {
      ctx.setTransform(1, 0, 0, 1, 0, 0);
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      ctx.scale(ratio, ratio);
      ctx.imageSmoothingEnabled = true;

      const pad = 24;
      const w = cssWidth;
      const h = cssHeight;
      const cx = w / 2;
      const cy = h / 2;
      const currentR = Number(rSelect.value || '1');
      const scale = (Math.min(w, h) / 2 - pad) / fixedExtent;

      const styles = getComputedStyle(document.documentElement);
      const axisColor = styles.getPropertyValue('--axis').trim() || '#4a3f40';
      const tickColor = styles.getPropertyValue('--tick').trim() || '#c7aeb3';
      const labelColor = styles.getPropertyValue('--muted').trim() || '#6f5d5f';
      const accent = styles.getPropertyValue('--accent').trim() || '#d16b8f';
      const hitColor = styles.getPropertyValue('--hit').trim() || '#2ecc71';
      const missColor = styles.getPropertyValue('--miss').trim() || '#c62828';

      // Draw axes
      ctx.strokeStyle = axisColor;
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(pad / 2, cy);
      ctx.lineTo(w - pad / 2, cy);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(cx, pad / 2);
      ctx.lineTo(cx, h - pad / 2);
      ctx.stroke();

      // Draw ticks and labels
      const ticks = [-fixedExtent, -fixedExtent / 2, 0, fixedExtent / 2, fixedExtent];
      ctx.strokeStyle = tickColor;
      ctx.fillStyle = labelColor;
      ctx.font = '12px system-ui, sans-serif';
      for (const t of ticks) {
        const px = cx + t * scale;
        const py = cy - t * scale;
        ctx.beginPath();
        ctx.moveTo(px, cy - 4);
        ctx.lineTo(px, cy + 4);
        ctx.stroke();
        ctx.textAlign = 'center';
        ctx.textBaseline = 'top';
        ctx.fillText(formatTick(t), px, cy + 8);

        ctx.beginPath();
        ctx.moveTo(cx - 4, py);
        ctx.lineTo(cx + 4, py);
        ctx.stroke();
        ctx.textAlign = 'left';
        ctx.textBaseline = 'middle';
        ctx.fillText(formatTick(t), cx + 8, py);
      }

      // Draw area shapes with translucent fill
      ctx.fillStyle = accent;
      ctx.globalAlpha = 0.28;

      // Rectangle in Q2 (quadrant II): x in [-R, 0], y in [0, R/2]
      rectPath(ctx, scale, cx, cy, -currentR, 0, 0, currentR / 2);
      ctx.fill();

      // Quarter circle in Q1 (quadrant I): center at origin, radius R/2
      ctx.beginPath();
      ctx.moveTo(cx, cy);
      ctx.arc(cx, cy, (currentR / 2) * scale, -0.5 * Math.PI, 0, false);
      ctx.closePath();
      ctx.fill();

      // Triangle in Q3 (quadrant III): vertices (-R/2, 0), (0, 0), (0, -R/2)
      ctx.beginPath();
      const triA = toPx(scale, cx, cy, -currentR / 2, 0);
      const triB = toPx(scale, cx, cy, 0, 0);
      const triC = toPx(scale, cx, cy, 0, -currentR / 2);
      ctx.moveTo(triA.x, triA.y);
      ctx.lineTo(triB.x, triB.y);
      ctx.lineTo(triC.x, triC.y);
      ctx.closePath();
      ctx.fill();
      ctx.globalAlpha = 1;

      // Draw history points (image updated after check per task requirement)
      if (!history || history.length === 0) return;
      for (const point of history) {
        const pt = toPx(scale, cx, cy, Number(point.x), Number(point.y));
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, 4, 0, Math.PI * 2);
        const sameR = Number(point.r) === currentR;
        ctx.globalAlpha = sameR ? 1 : 0.35;
        ctx.fillStyle = point.hit ? hitColor : missColor;
        ctx.fill();
        ctx.lineWidth = 1;
        ctx.strokeStyle = axisColor;
        ctx.stroke();
      }
    } finally {
      ctx.globalAlpha = 1;
      ctx.restore();
    }
  }

  /**
   * Draw a rectangle path
   */
  function rectPath(ctx, scale, cx, cy, x0, y0, x1, y1) {
    const a = toPx(scale, cx, cy, x0, y0);
    const b = toPx(scale, cx, cy, x1, y1);
    ctx.beginPath();
    ctx.moveTo(a.x, a.y);
    ctx.lineTo(b.x, a.y);
    ctx.lineTo(b.x, b.y);
    ctx.lineTo(a.x, b.y);
    ctx.closePath();
  }

  /**
   * Convert logical coordinates to pixel coordinates
   */
  function toPx(scale, cx, cy, x, y) {
    return { x: cx + x * scale, y: cy - y * scale };
  }

  /**
   * Format tick labels
   */
  function formatTick(value) {
    return Number.isInteger(value) ? value.toString() : value.toFixed(1);
  }
})();
