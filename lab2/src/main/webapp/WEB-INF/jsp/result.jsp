<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!doctype html>
<html lang="ru">
  <head>
    <meta charset="utf-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>Результаты проверки · ЛР2</title>
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
      }

      * { box-sizing: border-box; }

      body {
        margin: 0;
        font-family: sans-serif;
        background: var(--bg);
        color: var(--fg);
        font-size: 16px;
      }

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
        color: #4a3f40;
      }

      #page-header h1::after {
        content: ' · Результаты';
        color: var(--accent);
        font-weight: 400;
      }

      #page-header .student {
        margin-top: 4px;
        font-size: 13px;
        color: var(--muted);
      }

      main {
        padding: 16px;
        max-width: 900px;
        margin: 0 auto;
      }

      section {
        background: var(--panel);
        border: 1px solid var(--border);
        border-radius: 12px;
        padding: 16px;
        margin-bottom: 16px;
      }

      h2 {
        margin: 0 0 12px 0;
        font-size: 18px;
        color: var(--accent-strong);
      }

      table {
        width: 100%;
        border-collapse: collapse;
        font-size: 14px;
      }

      th, td {
        padding: 10px 12px;
        border-bottom: 1px solid var(--border);
        text-align: left;
      }

      th {
        color: var(--accent-strong);
        font-weight: 600;
        background: rgba(209, 107, 143, 0.08);
      }

      td.ok {
        color: var(--hit);
        font-weight: 600;
      }

      td.fail {
        color: var(--miss);
        font-weight: 600;
      }

      .btn {
        display: inline-block;
        padding: 10px 16px;
        border-radius: 10px;
        border: 1px solid var(--border);
        background: #fff7f8;
        color: var(--fg);
        text-decoration: none;
        font-size: 15px;
        cursor: pointer;
        transition: 0.15s ease;
        margin-top: 12px;
      }

      .btn:hover {
        border-color: var(--accent);
        color: var(--accent-strong);
        box-shadow: 0 3px 10px rgba(209, 107, 143, 0.18);
      }
    </style>
  </head>
  <body>
    <header id="page-header">
      <h1>Веб-программирование</h1>
      <div class="student">Седов Даниил Борисович • P3216 • Вариант 409529</div>
    </header>

    <main>
      <section>
        <h2>Результаты проверки</h2>
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
            <c:forEach var="row" items="${newResults}">
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

        <a class="btn" href="${pageContext.request.contextPath}/controller">Вернуться к форме</a>
      </section>
    </main>
  </body>
</html>
