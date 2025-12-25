package itmo.web.lab2;

import itmo.web.lab2.core.Result;
import itmo.web.lab2.core.SessionStore;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * Front controller servlet.
 * All requests from web pages go through this servlet.
 * Determines request type and delegates:
 * - If coordinates present → forward to AreaCheckServlet
 * - Otherwise → forward to JSP form page
 */
@WebServlet(name = "ControllerServlet", urlPatterns = {"/controller"})
public class ControllerServlet extends HttpServlet {
    private SessionStore sessionStore;

    @Override
    public void init() {
        this.sessionStore = new SessionStore(Constants.SESSION_RESULTS_KEY, Constants.HISTORY_LIMIT);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        handle(req, resp);
    }

    private void handle(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Check if request contains coordinate information
        if (hasCoordinateParams(req)) {
            // Delegate to AreaCheckServlet for processing
            req.getRequestDispatcher("/area-check").forward(req, resp);
            return;
        }

        // No coordinates - show the form page
        HttpSession session = req.getSession(true);
        List<Result> history = sessionStore.snapshot(session);
        ViewHelper.attachHistory(req, history);
        req.setAttribute(ViewHelper.ATTR_ERRORS, Collections.emptyList());
        preserveFormDefaults(req);
        ViewHelper.forwardToForm(req, resp);
    }

    private static boolean hasCoordinateParams(HttpServletRequest req) {
        boolean hasX = hasText(req.getParameter("x"));
        boolean hasR = hasText(req.getParameter("r"));
        String[] ys = req.getParameterValues("y");
        boolean hasY = ys != null && ys.length > 0;
        boolean hasClickY = hasText(req.getParameter("yClick"));
        return hasX || hasR || hasY || hasClickY;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static void preserveFormDefaults(HttpServletRequest req) {
        if (req.getAttribute(ViewHelper.ATTR_FORM_X) == null) {
            req.setAttribute(ViewHelper.ATTR_FORM_X, "");
        }
        if (req.getAttribute(ViewHelper.ATTR_FORM_Y) == null) {
            req.setAttribute(ViewHelper.ATTR_FORM_Y, List.of());
        }
        if (req.getAttribute(ViewHelper.ATTR_FORM_R) == null) {
            req.setAttribute(ViewHelper.ATTR_FORM_R, "");
        }
    }
}
