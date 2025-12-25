package itmo.web.lab2;

import itmo.web.lab2.core.Result;
import itmo.web.lab2.util.Json;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ViewHelper {
    private ViewHelper() {
    }

    private static final String FORM_VIEW_PATH = "/WEB-INF/jsp/index.jsp";
    private static final String RESULT_VIEW_PATH = "/WEB-INF/jsp/result.jsp";

    public static final String ATTR_HISTORY = "history";
    public static final String ATTR_HISTORY_JSON = "historyJson";
    public static final String ATTR_ERRORS = "errors";
    public static final String ATTR_NEW_RESULTS = "newResults";
    public static final String ATTR_FORM_X = "formX";
    public static final String ATTR_FORM_R = "formR";
    public static final String ATTR_FORM_Y = "formY";

    public static void attachHistory(HttpServletRequest request, List<Result> history) {
        request.setAttribute(ATTR_HISTORY, history);
        request.setAttribute(ATTR_HISTORY_JSON, buildHistoryJson(history));
    }

    /**
     * Forward to the form page (index.jsp).
     * Used by ControllerServlet for requests without coordinates.
     */
    public static void forwardToForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher(FORM_VIEW_PATH);
        dispatcher.forward(request, response);
    }

    /**
     * Forward to the result page (result.jsp).
     * Used by AreaCheckServlet after processing coordinates.
     */
    public static void forwardToResult(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RequestDispatcher dispatcher = request.getRequestDispatcher(RESULT_VIEW_PATH);
        dispatcher.forward(request, response);
    }

    private static String buildHistoryJson(List<Result> history) {
        List<String> items = new ArrayList<>(history.size());
        for (Result r : history) {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("x", Json.str(r.xDisplay()));
            fields.put("y", Json.str(r.yDisplay()));
            fields.put("r", Json.str(Integer.toString(r.r())));
            fields.put("hit", Json.bool(r.hit()));
            fields.put("timestamp", Json.str(r.timestamp()));
            fields.put("durationMs", Json.str(r.durationMillisFormatted()));
            items.add(Json.obj(fields));
        }
        return Json.arr(items);
    }
}
