package itmo.web.lab1.core;

import java.math.BigDecimal;

public record Result(int x, BigDecimal y, int r, boolean hit, String timestamp, long durationNs) {}
