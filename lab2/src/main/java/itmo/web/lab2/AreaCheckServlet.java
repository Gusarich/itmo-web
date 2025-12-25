package itmo.web.lab2;

import itmo.web.lab2.core.HitTest;
import itmo.web.lab2.core.Result;
import itmo.web.lab2.core.SessionStore;
import itmo.web.lab2.core.Validator;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Servlet for checking point hits.
 * Processes all requests containing coordinate information.
 * Returns result page with parameters table, hit/miss result, and link back to form.
 */
@WebServlet(name = "AreaCheckServlet", urlPatterns = "/area-check")
public class AreaCheckServlet extends HttpServlet {
    private SessionStore sessionStore;
    private DateTimeFormatter isoFormatter;

    @Override
    public void init() {
        this.sessionStore = new SessionStore(Constants.SESSION_RESULTS_KEY, Constants.HISTORY_LIMIT);
        this.isoFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.systemDefault());
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    private void process(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        long start = System.nanoTime();
        String now = isoFormatter.format(Instant.now());
        HttpSession session = req.getSession(true);

        List<String> errors = new ArrayList<>();
        String rawX = req.getParameter("x");
        String rawR = req.getParameter("r");
        String[] checkboxY = req.getParameterValues("y");
        String clickY = req.getParameter("yClick");

        // Preserve form values for error display
        List<String> selectedCheckboxes = checkboxY == null
                ? List.of()
                : Arrays.asList(checkboxY);
        req.setAttribute(ViewHelper.ATTR_FORM_X, rawX != null ? rawX : "");
        req.setAttribute(ViewHelper.ATTR_FORM_R, rawR != null ? rawR : "");
        req.setAttribute(ViewHelper.ATTR_FORM_Y, selectedCheckboxes);

        // Collect Y values from checkboxes and click
        List<String> yCandidates = new ArrayList<>();
        if (checkboxY != null) {
            yCandidates.addAll(Arrays.asList(checkboxY));
        }
        if (clickY != null && !clickY.isBlank()) {
            yCandidates.add(0, clickY);
        }
        if (!yCandidates.isEmpty()) {
            Set<String> deduped = new LinkedHashSet<>(yCandidates);
            yCandidates = new ArrayList<>(deduped);
        }

        if (yCandidates.isEmpty()) {
            errors.add("Выберите хотя бы одно значение Y.");
        }

        // Validate X and R
        BigDecimal parsedX = Validator.parseX(rawX, errors);
        Integer parsedR = Validator.parseR(rawR, errors);

        // Validate all Y values
        List<BigDecimal> parsedYs = new ArrayList<>();
        if (errors.isEmpty()) {
            for (String yRaw : yCandidates) {
                BigDecimal yVal = Validator.parseY(yRaw, errors);
                if (errors.isEmpty() && yVal != null) {
                    parsedYs.add(yVal);
                } else {
                    break;
                }
            }
        }

        // If validation failed, return to form page with errors
        if (!errors.isEmpty()) {
            req.setAttribute(ViewHelper.ATTR_ERRORS, errors);
            ViewHelper.attachHistory(req, sessionStore.snapshot(session));
            ViewHelper.forwardToForm(req, resp);
            return;
        }

        // Compute hits for all valid points
        List<Result> newResults = new ArrayList<>();
        if (parsedX != null && parsedR != null && !parsedYs.isEmpty()) {
            BigDecimal rBig = BigDecimal.valueOf(parsedR);
            for (BigDecimal yVal : parsedYs) {
                boolean hit = HitTest.isHit(parsedX, yVal, rBig);
                Result result = new Result(parsedX, yVal, parsedR, hit, now, System.nanoTime() - start);
                newResults.add(result);
            }
        }

        // Store results in session
        sessionStore.addAll(session, newResults);

        // Forward to result page with the new results
        req.setAttribute(ViewHelper.ATTR_NEW_RESULTS, newResults);
        ViewHelper.forwardToResult(req, resp);
    }
}
