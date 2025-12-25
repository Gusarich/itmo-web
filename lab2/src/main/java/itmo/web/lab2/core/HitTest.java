package itmo.web.lab2.core;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class HitTest {
    private HitTest() {
    }

    private static final MathContext MC = new MathContext(34, RoundingMode.HALF_EVEN);
    private static final BigDecimal TWO = new BigDecimal("2");

    public static boolean isHit(BigDecimal x, BigDecimal y, BigDecimal r) {
        BigDecimal zero = BigDecimal.ZERO;

        // Rectangle: quadrant II mirror, width R, height R/2
        boolean rect = le(x, zero) && ge(x, r.negate(MC)) && ge(y, zero) && le(y, r.divide(TWO, MC));

        BigDecimal x2 = x.multiply(x, MC);
        BigDecimal y2 = y.multiply(y, MC);
        BigDecimal rsqOver4 = r.multiply(r, MC).divide(new BigDecimal("4"), MC);
        // Quarter circle in Q1
        boolean quarter = ge(x, zero) && ge(y, zero) && le(x2.add(y2, MC), rsqOver4);

        // Triangle in Q3: legs R/2 along negative axes, hypotenuse through (-R/2,0) and (0,-R/2)
        BigDecimal halfR = r.divide(TWO, MC);
        BigDecimal minusHalfR = halfR.negate(MC);
        BigDecimal rhs = x.negate(MC).subtract(halfR, MC); // y >= -x - R/2
        boolean tri = ge(x, minusHalfR) && le(x, zero) && le(y, zero) && ge(y, rhs);

        return rect || quarter || tri;
    }

    private static boolean ge(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) >= 0;
    }

    private static boolean le(BigDecimal a, BigDecimal b) {
        return a.compareTo(b) <= 0;
    }
}
