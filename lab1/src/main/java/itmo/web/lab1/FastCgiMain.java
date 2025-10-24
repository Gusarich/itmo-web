package itmo.web.lab1;

import com.fastcgi.FCGIInterface;
import itmo.web.lab1.core.*;
import itmo.web.lab1.util.Json;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * FastCGI entrypoint using the classic FCGIaccept() loop from the FastCGI Java kit.
 */
public class FastCgiMain {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());
    // Server-side session history (per SID cookie)
    private static final SessionStore SESSIONS = new SessionStore(100);

    public static void main(String[] args) {
        String port = Optional.ofNullable(System.getProperty("FCGI_PORT"))
                .orElseGet(() -> Optional.ofNullable(System.getenv("FCGI_PORT")).orElse("49953"));
        System.setProperty("FCGI_PORT", port);
        System.setProperty("portNum", port);
        System.out.println("[FastCGI] Listening port=" + port);

        FCGIInterface fcgi = new FCGIInterface();
        while (true) {
            if (!FCGIInterface.isFCGI && FCGIInterface.srvSocket == null) {
                sleepQuietly(50);
                fcgi = new FCGIInterface();
                continue;
            }
            try {
                int rc = fcgi.FCGIaccept();
                if (rc < 0) {
                    sleepQuietly(50);
                    continue;
                }
                handleRequest();
            } catch (Throwable t) {
                System.err.println("[FastCGI] Accept loop issue: " + t);
                sleepQuietly(100);
                fcgi = new FCGIInterface();
            }
        }
    }

    private static void handleRequest() {
        long start = System.nanoTime();
        String now = ISO.format(Instant.now());
        PrintWriter out = new PrintWriter(new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true);
        try {
            String method = System.getProperty("REQUEST_METHOD", "GET");
            String scriptName = System.getProperty("SCRIPT_NAME", "");
            String requestUri = System.getProperty("REQUEST_URI", scriptName.isEmpty() ? "/" : scriptName);
            String path = !scriptName.isEmpty() ? scriptName : requestUri;
            String query = System.getProperty("QUERY_STRING", "");
            String cookieHeader = System.getProperty("HTTP_COOKIE", "");
            String sid = findCookie(cookieHeader, "SID");
            boolean needSetCookie = false;
            if (sid == null || sid.isBlank()) {
                sid = UUID.randomUUID().toString().replace("-", "");
                needSetCookie = true;
            }
            // No session id needed

            if (!"GET".equalsIgnoreCase(method)) {
                long durNs = elapsedNs(start);
                writeHeaders(out, 405, needSetCookie ? cookieFor(sid) : null);
                out.print(Json.obj(Map.of(
                        "errors", Json.arr(List.of(Json.str("Допускается только GET-запрос."))),
                        "nowIso", Json.str(now),
                        "durationMs", formatDurationMillis(durNs)
                )));
                out.flush();
                return;
            }

            if (!"/api/check".equals(path)) {
                long durNs = elapsedNs(start);
                writeHeaders(out, 404, needSetCookie ? cookieFor(sid) : null);
                out.print(Json.obj(Map.of(
                        "errors", Json.arr(List.of(Json.str("Маршрут не найден."))),
                        "nowIso", Json.str(now),
                        "durationMs", formatDurationMillis(durNs)
                )));
                out.flush();
                return;
            }

            // /api/check: parse inputs
            Map<String, String> q = parseQuery(query);
            if (q.isEmpty() || (q.keySet().stream().noneMatch(k -> k.equals("x") || k.equals("y") || k.equals("r")))) {
                long durNs = elapsedNs(start);
                writeHeaders(out, 200, needSetCookie ? cookieFor(sid) : null);
                Map<String, String> payload = new LinkedHashMap<>();
                payload.put("nowIso", Json.str(now));
                payload.put("durationMs", formatDurationMillis(durNs));
                payload.put("history", Json.arr(historyJson(sid)));
                out.print(Json.obj(payload));
                out.flush();
                return;
            }

            var parsed = Validator.parse(q.get("x"), q.get("y"), q.get("r"));

            if (parsed instanceof Validator.Either.Left left) {
                long durNs = elapsedNs(start);
                writeHeaders(out, 400, needSetCookie ? cookieFor(sid) : null);
                out.print(Json.obj(Map.of(
                        "errors", Json.arr(left.errors().stream().map(Json::str).toList()),
                        "nowIso", Json.str(now),
                        "durationMs", formatDurationMillis(durNs)
                )));
                out.flush();
                return;
            }

            var ok = ((Validator.Either.Right) parsed).value();
            boolean hit = HitTest.isHit(new BigDecimal(ok.x()), ok.y(), new BigDecimal(ok.r()));
            long durNs = elapsedNs(start);
            Result result = new Result(ok.x(), ok.y(), ok.r(), hit, now, durNs);
            // persist in session history
            SESSIONS.add(sid, result);

            // build history JSON array
            List<String> histItems = historyJson(sid);

            writeHeaders(out, 200, needSetCookie ? cookieFor(sid) : null);
            Map<String, String> payload = new LinkedHashMap<>();
            payload.put("x", Integer.toString(ok.x()));
            payload.put("y", Json.str(ok.y().toPlainString()));
            payload.put("r", Integer.toString(ok.r()));
            payload.put("hit", Json.bool(hit));
            payload.put("nowIso", Json.str(now));
            payload.put("durationMs", formatDurationMillis(durNs));
            payload.put("history", Json.arr(histItems));
            out.print(Json.obj(payload));
            out.flush();
        } catch (Throwable t) {
            long durNs = elapsedNs(start);
            writeHeaders(out, 500, null);
            String msg = t.getClass().getSimpleName() + (t.getMessage() != null ? (": " + t.getMessage()) : "");
            out.print(Json.obj(Map.of(
                    "errors", Json.arr(List.of(Json.str("Внутренняя ошибка."), Json.str(msg))),
                    "nowIso", Json.str(now),
                    "durationMs", formatDurationMillis(durNs)
            )));
            out.flush();
        }
    }

    private static void writeHeaders(PrintWriter out, int code, String setCookie) {
        String statusText = switch (code) {
            case 200 -> "200 OK";
            case 400 -> "400 Bad Request";
            case 404 -> "404 Not Found";
            case 405 -> "405 Method Not Allowed";
            default -> "500 Internal Server Error";
        };
        out.print("Status: " + statusText + "\r\n");
        out.print("Cache-Control: no-store, no-cache, must-revalidate\r\n");
        if (setCookie != null) out.print("Set-Cookie: " + setCookie + "\r\n");
        out.print("Content-type: application/json; charset=utf-8\r\n");
        out.print("\r\n");
    }

    private static String resultToJson(Result r) {
        return Json.obj(Map.of(
                "x", Integer.toString(r.x()),
                "y", Json.str(r.y().toPlainString()),
                "r", Integer.toString(r.r()),
                "hit", Json.bool(r.hit()),
                "timestamp", Json.str(r.timestamp()),
                "durationMs", formatDurationMillis(r.durationNs())
        ));
    }

    private static List<String> historyJson(String sid) {
        List<Result> snapshot = SESSIONS.snapshot(sid);
        List<String> histItems = new ArrayList<>(snapshot.size());
        for (var r : snapshot) histItems.add(resultToJson(r));
        return histItems;
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> m = new LinkedHashMap<>();
        if (raw == null || raw.isEmpty()) return m;
        for (String p : raw.split("&")) {
            int i = p.indexOf('=');
            String k = i >= 0 ? p.substring(0, i) : p;
            String v = i >= 0 ? p.substring(i + 1) : "";
            m.put(urlDecode(k), urlDecode(v));
        }
        return m;
    }

    private static String urlDecode(String s) {
        try { return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }

    private static String findCookie(String cookieHeader, String name) {
        if (cookieHeader == null || cookieHeader.isEmpty()) return null;
        String[] parts = cookieHeader.split(";\\s*");
        for (String p : parts) {
            int i = p.indexOf('=');
            if (i <= 0) continue;
            String k = p.substring(0, i).trim();
            if (name.equals(k)) return p.substring(i + 1).trim();
        }
        return null;
    }

    private static String cookieFor(String sid) {
        // 30 days
        return "SID=" + sid + "; Path=/; HttpOnly; SameSite=Lax; Max-Age=" + (30L*24*60*60);
    }

    private static long elapsedNs(long startNano) {
        return Math.max(0, System.nanoTime() - startNano);
    }

    private static String formatDurationMillis(long durationNs) {
        double millis = durationNs / 1_000_000.0;
        return String.format(Locale.US, "%.3f", millis);
    }

    private static void sleepQuietly(long millis) {
        try { Thread.sleep(millis); }
        catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }
}
