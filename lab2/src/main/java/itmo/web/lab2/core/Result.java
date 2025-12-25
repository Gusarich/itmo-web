package itmo.web.lab2.core;

import java.math.BigDecimal;
import java.util.Locale;

public record Result(BigDecimal x, BigDecimal y, int r, boolean hit, String timestamp, long durationNs) {
    public String durationMillisFormatted() {
        double millis = durationNs / 1_000_000.0;
        return String.format(Locale.US, "%.3f", millis);
    }

    public String xDisplay() {
        return normalize(x);
    }

    public String yDisplay() {
        return normalize(y);
    }

    private static String normalize(BigDecimal value) {
        if (value == null) {
            return "";
        }
        return value.stripTrailingZeros().toPlainString();
    }
}
