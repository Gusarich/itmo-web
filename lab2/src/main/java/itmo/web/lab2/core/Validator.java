package itmo.web.lab2.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public final class Validator {
    private Validator() {
    }

    public record Parsed(BigDecimal x, BigDecimal y, int r) {
    }

    private static final BigDecimal MIN_X = BigDecimal.valueOf(-3);
    private static final BigDecimal MAX_X = BigDecimal.valueOf(3);
    private static final BigDecimal MIN_Y = BigDecimal.valueOf(-3);
    private static final BigDecimal MAX_Y = BigDecimal.valueOf(3);
    private static final int[] ALLOWED_R = {1, 2, 3, 4, 5};
    private static final MathContext MC = MathContext.DECIMAL128;

    public static Either parsePoint(String rawX, String rawY, String rawR) {
        List<String> errors = new ArrayList<>();

        BigDecimal x = parseX(rawX, errors);
        BigDecimal y = parseY(rawY, errors);
        Integer r = parseR(rawR, errors);

        if (!errors.isEmpty() || x == null || y == null || r == null) {
            return Either.left(errors);
        }
        return Either.right(new Parsed(x, y, r));
    }

    public static BigDecimal parseX(String rawX, List<String> errors) {
        return parseCoordinate(rawX, MIN_X, MAX_X, "X", errors);
    }

    public static BigDecimal parseY(String rawY, List<String> errors) {
        return parseCoordinate(rawY, MIN_Y, MAX_Y, "Y", errors);
    }

    public static Integer parseR(String rawR, List<String> errors) {
        return parseRadius(rawR, errors);
    }

    private static BigDecimal parseCoordinate(String raw,
                                               BigDecimal min,
                                               BigDecimal max,
                                               String name,
                                               List<String> errors) {
        if (raw == null || raw.isBlank()) {
            errors.add(name + " не задан.");
            return null;
        }
        String normalized = normalize(raw);
        BigDecimal value;
        try {
            value = new BigDecimal(normalized, MC);
        } catch (NumberFormatException ex) {
            errors.add("Введите число для " + name + " (можно точку или запятую).");
            return null;
        }
        if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            errors.add(String.format(Locale.ROOT,
                    "%s должен быть в диапазоне от %s до %s.",
                    name,
                    min.stripTrailingZeros().toPlainString(),
                    max.stripTrailingZeros().toPlainString()));
        }
        return value;
    }

    private static Integer parseRadius(String raw, List<String> errors) {
        if (raw == null || raw.isBlank()) {
            errors.add("R не задан.");
            return null;
        }
        try {
            int value = Integer.parseInt(Objects.requireNonNullElse(raw, "").trim());
            boolean allowed = false;
            for (int option : ALLOWED_R) {
                if (option == value) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed || value <= 0) {
                errors.add("Значение R должно быть в диапазоне от 1 до 5 и больше 0.");
                return null;
            }
            return value;
        } catch (NumberFormatException ex) {
            errors.add("R должен быть целым числом от 1 до 5.");
            return null;
        }
    }

    private static String normalize(String raw) {
        return raw.trim().replace(',', '.');
    }

    public sealed interface Either permits Either.Left, Either.Right {
        record Left(List<String> errors) implements Either {
        }

        record Right(Parsed value) implements Either {
        }

        static Either left(List<String> errors) {
            return new Left(errors);
        }

        static Either right(Parsed value) {
            return new Right(value);
        }
    }
}
