<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<!doctype html>
<html lang="ru">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Веб-программирование · ЛР2</title>
    <style>
      :root {
        --bg: #fffafa;
        --panel: #fff3f5;
        --fg: #312b2b;
        --muted: #6f5d5f;
        --accent: #d16b8f;
        --accent-strong: #b44d74;
        --hit: #2ecc71;
        --miss: #c62828;
        --border: #f0d9dd;
        --tick: #c7aeb3;
        --axis: #4a3f40;
      }

      * { box-sizing: border-box; }

      body {
        margin: 0;
        font-family: system-ui, -apple-system, 'Segoe UI', sans-serif;
        background: var(--bg);
        color: var(--fg);
      }

      /* Header: explicit sans-serif font, color, size per requirements */
      #page-header {
        padding: 12px 16px;
        border-bottom: 1px solid var(--border);
        font-family: sans-serif;
        color: var(--fg);
        font-size: 16px;
      }

      #page-header h1 {
        margin: 0;
        font-size: 20px;
        font-weight: 650;
        color: var(--axis);
      }

      /* Pseudo-element selector */
      #page-header h1::after {
        content: ' · ЛР2';
        color: var(--accent);
        font-weight: 400;
      }

      /* Descendant selector */
      #page-header .student {
        margin-top: 4px;
        font-size: 13px;
        color: var(--muted);
      }

      main {
        display: grid;
        grid-template-columns: repeat(auto-fit, minmax(360px, 1fr));
        gap: 16px;
        padding: 16px;
      }

      section.panel {
        background: var(--panel);
        border: 1px solid var(--border);
        border-radius: 12px;
        padding: 12px;
        box-shadow: 0 6px 18px rgba(209, 107, 143, 0.08);
      }

      h2 { margin-top: 0; }

      /* Form field styling */
      form .field { margin-bottom: 12px; }

      label { font-weight: 600; display: block; margin-bottom: 4px; }

      /* Input margins in pixels as required */
      input[type="text"] {
        width: 100%;
        padding: 9px 12px;
        margin: 6px 0;
        border-radius: 8px;
        border: 1px solid var(--border);
        background: #fff7f8;
        font-size: 15px;
      }

      /* Pseudo-class selectors */
      input[type="text"]:focus {
        border-color: var(--accent);
        outline: none;
        box-shadow: 0 0 0 3px rgba(209, 107, 143, 0.18);
      }

      select {
        width: 100%;
        padding: 9px 12px;
        margin: 6px 0;
        border-radius: 8px;
        border: 1px solid var(--border);
        background: #fff7f8;
        font-size: 15px;
      }

      select:focus {
        border-color: var(--accent);
        outline: none;
        box-shadow: 0 0 0 3px rgba(209, 107, 143, 0.18);
      }

      .checkbox-grid {
        display: grid;
        grid-template-columns: repeat(3, minmax(0, 1fr));
        gap: 6px;
        margin: 6px 0;
      }

      .checkbox-grid label {
        font-weight: 500;
        display: flex;
        gap: 6px;
        align-items: center;
        background: #fff7f8;
        border: 1px solid var(--border);
        border-radius: 8px;
        padding: 6px 8px;
      }

      .checkbox-grid label:hover {
        border-color: var(--accent);
      }

      .btn {
        padding: 9px 12px;
        margin: 6px 0;
        border-radius: 10px;
        border: 1px solid var(--border);
        background: #fff7f8;
        cursor: pointer;
        font-size: 15px;
        transition: 0.15s ease;
      }

      .btn:hover {
        border-color: var(--accent);
        color: var(--accent-strong);
      }

      .btn:focus {
        outline: 2px solid var(--accent);
        outline-offset: 2px;
      }

      #errors {
        min-height: 24px;
        color: #a74a3f;
        margin-top: 6px;
      }

      #errors ul {
        margin: 0;
        padding-left: 20px;
      }

      #plot canvas {
        width: 100%;
        max-width: 640px;
        aspect-ratio: 1 / 1;
        border: 1px solid var(--border);
        border-radius: 10px;
        background: #fffdfd;
        cursor: crosshair;
      }

      .legend {
        font-size: 12px;
        color: var(--muted);
        margin-top: 8px;
      }

      /* Results table styling */
      #results {
        margin: 16px;
        padding: 16px;
        background: var(--panel);
        border: 1px solid var(--border);
        border-radius: 12px;
        box-shadow: 0 6px 18px rgba(49, 43, 43, 0.04);
      }

      table {
        width: 100%;
        border-collapse: collapse;
        font-size: 14px;
      }

      th, td {
        padding: 8px;
        border-bottom: 1px solid var(--border);
        text-align: left;
      }

      /* ID selector */
      #results th { color: var(--accent-strong); }

      td.ok { color: var(--hit); font-weight: 600; }
      td.fail { color: var(--miss); font-weight: 600; }

      .actions { display: flex; gap: 8px; flex-wrap: wrap; }

      @media (max-width: 768px) {
        main { grid-template-columns: 1fr; }
      }
    </style>
    <script defer src="${pageContext.request.contextPath}/static/js/app.js"></script>
  </head>
  <body>
    <header id="page-header">
      <h1>Веб-программирование</h1>
      <div class="student">Седов Даниил Борисович • P3216 • Вариант 409529</div>
    </header>

    <main>
      <section class="panel">
        <h2>Ввод данных</h2>
        <form id="hit-form" method="post" action="${pageContext.request.contextPath}/controller">
          <div class="field">
            <label for="x-input">X (от -3 до 3):</label>
            <input id="x-input" name="x" type="text" value="${fn:escapeXml(formX)}" autocomplete="off" />
          </div>

          <div class="field">
            <label>Y (можно выбрать несколько):</label>
            <div class="checkbox-grid">
              <c:set var="allowedY" value="${fn:split('-2,-1.5,-1,-0.5,0,0.5,1,1.5,2', ',')}" />
              <c:forEach var="yVal" items="${allowedY}">
                <label>
                  <input type="checkbox" name="y" value="${yVal}" <c:if test="${fn:contains(formY, yVal)}">checked</c:if> />
                  <span>${yVal}</span>
                </label>
              </c:forEach>
            </div>
          </div>

          <div class="field">
            <label for="r-select">R:</label>
            <select id="r-select" name="r">
              <option value="">-- выберите R --</option>
              <c:forEach begin="1" end="5" var="rVal">
                <option value="${rVal}" <c:if test="${formR == rVal}">selected</c:if>>${rVal}</option>
              </c:forEach>
            </select>
          </div>

          <div class="actions">
            <button type="submit" class="btn" id="submit-btn">Проверить</button>
            <button type="button" class="btn" id="random-btn">Случайная точка</button>
          </div>

          <input type="hidden" id="y-click" name="yClick" />

          <div id="errors">
            <c:if test="${not empty errors}">
              <ul>
                <c:forEach var="err" items="${errors}">
                  <li>${fn:escapeXml(err)}</li>
                </c:forEach>
              </ul>
            </c:if>
          </div>
        </form>
      </section>

      <section class="panel" id="plot">
        <h2>График области</h2>
        <canvas id="plot-canvas" width="640" height="640" aria-label="Интерактивный график области"></canvas>
        <p class="legend">Клик по графику отправляет координаты на сервер (если задан R).</p>
      </section>
    </main>

    <section id="results">
      <h2>Таблица результатов (сессия)</h2>
      <table>
        <thead>
          <tr>
            <th>X</th>
            <th>Y</th>
            <th>R</th>
            <th>Попадание</th>
            <th>Время</th>
            <th>Выполнение, мс</th>
          </tr>
        </thead>
        <tbody>
          <c:forEach var="row" items="${history}">
            <tr>
              <td>${row.xDisplay()}</td>
              <td>${row.yDisplay()}</td>
              <td>${row.r()}</td>
              <td class="${row.hit() ? 'ok' : 'fail'}">
                <c:choose>
                  <c:when test="${row.hit()}">Да</c:when>
                  <c:otherwise>Нет</c:otherwise>
                </c:choose>
              </td>
              <td>${row.timestamp()}</td>
              <td>${row.durationMillisFormatted()}</td>
            </tr>
          </c:forEach>
        </tbody>
      </table>
    </section>

    <script id="history-data" type="application/json">
      <c:out value="${historyJson}" escapeXml="false" />
    </script>
  </body>
</html>
