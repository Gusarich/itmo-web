package itmo.web.lab1.core;

import java.math.BigDecimal;
import java.util.*;

public final class Validator {
    private Validator() {}

    public record Parsed(int x, BigDecimal y, int r) {}

    private static final Set<Integer> ALLOWED_X = Set.of(-5,-4,-3,-2,-1,0,1,2,3);
    private static final Set<Integer> ALLOWED_R = Set.of(1,2,3,4,5);

    public static Either parse(String xRaw, String yRaw, String rRaw) {
        List<String> errs = new ArrayList<>();

        Integer x = null; Integer r = null; BigDecimal y = null;

        // X
        try {
            x = Integer.valueOf(Objects.requireNonNullElse(xRaw, ""));
        } catch (Exception e) { errs.add("X должен быть целым числом от -5 до 3."); }
        if (x != null && !ALLOWED_X.contains(x)) errs.add("Значение X должно быть в диапазоне от -5 до 3.");

        // R
        try {
            r = Integer.valueOf(Objects.requireNonNullElse(rRaw, ""));
        } catch (Exception e) { errs.add("R должен быть целым числом от 1 до 5."); }
        if (r != null && (!ALLOWED_R.contains(r) || r <= 0)) errs.add("Значение R должно быть в диапазоне от 1 до 5 и больше 0.");

        // Y (comma or dot, inclusive range)
        if (yRaw == null || yRaw.isBlank()) {
            errs.add("Y не задан.");
        } else {
            String norm = yRaw.trim().replace(',', '.');
            try {
                y = new java.math.BigDecimal(norm);
            } catch (Exception e) {
                errs.add("Введите число для Y (можно использовать точку или запятую).");
            }
            if (y != null) {
                if (y.compareTo(java.math.BigDecimal.valueOf(-3)) < 0 || y.compareTo(java.math.BigDecimal.valueOf(3)) > 0) {
                    errs.add("Значение Y должно быть в диапазоне от -3 до 3 включительно.");
                }
            }
        }

        if (!errs.isEmpty()) return Either.left(errs);
        return Either.right(new Parsed(x, y, r));
    }

    public static sealed interface Either permits Either.Left, Either.Right {
        record Left(List<String> errors) implements Either {}
        record Right(Parsed value) implements Either {}

        static Either Left(List<String> errors) { return new Left(errors); }
        static Either Right(Parsed value) { return new Right(value); }

        static Either left(List<String> errors) { return new Left(errors); }
        static Either right(Parsed value) { return new Right(value); }
    }
}
