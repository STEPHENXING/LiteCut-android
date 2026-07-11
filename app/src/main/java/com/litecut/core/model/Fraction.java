package com.litecut.core.model;

import java.util.Locale;

public final class Fraction {
    public final int numerator;
    public final int denominator;

    public Fraction(int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator == 0 ? 1 : denominator;
    }

    public static Fraction parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return new Fraction(0, 1);
        }
        String[] parts = value.split("/");
        if (parts.length == 2) {
            return new Fraction(parseInt(parts[0]), parseInt(parts[1]));
        }
        return new Fraction(parseInt(value), 1);
    }

    public double toDouble() {
        return denominator == 0 ? 0.0 : (double) numerator / (double) denominator;
    }

    public boolean sameValue(Fraction other) {
        return other != null && Math.abs(toDouble() - other.toDouble()) < 0.001;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "%d/%d", numerator, denominator);
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }
}
