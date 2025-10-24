package itmo.web.lab1.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class HitTest {
    private HitTest() {}

    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_EVEN);
    private static final BigDecimal TWO = new BigDecimal("2");

    public static boolean isHit(BigDecimal x, BigDecimal y, BigDecimal r) {
        // Rectangle: 0 <= x <= R, 0 <= y <= R/2
        boolean rect = ge(x, bd(0)) && le(x, r) && ge(y, bd(0)) && le(y, r.divide(TWO, MC));

        // Quarter circle in Q2: x<=0, y>=0, x^2 + y^2 <= (R/2)^2
        BigDecimal x2 = x.multiply(x, MC);
        BigDecimal y2 = y.multiply(y, MC);
        BigDecimal rsqOver4 = r.multiply(r, MC).divide(new BigDecimal("4"), MC);
        boolean quarter = le(x, bd(0)) && ge(y, bd(0)) && le(x2.add(y2, MC), rsqOver4);

        // Triangle in Q3: -R <= x <= 0, y <= 0, y >= -x/2 - R/2
        BigDecimal minusR = r.negate(MC);
        BigDecimal rhs = x.negate(MC).divide(TWO, MC).subtract(r.divide(TWO, MC), MC);
        boolean tri = ge(x, minusR) && le(x, bd(0)) && le(y, bd(0)) && ge(y, rhs);

        return rect || quarter || tri;
    }

    private static boolean ge(BigDecimal a, BigDecimal b) { return a.compareTo(b) >= 0; }
    private static boolean le(BigDecimal a, BigDecimal b) { return a.compareTo(b) <= 0; }
    private static BigDecimal bd(int v) { return BigDecimal.valueOf(v); }
}

