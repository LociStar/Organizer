package com.locibot.organizer2.utils;

import reactor.util.annotation.Nullable;

public class NumberUtil {

    /**
     * @param str The string to parse as an integer, may be {@code null}.
     * @return The string parsed as an integer or {@code null} if the string is not a valid representation of
     * a number.
     */
    @Nullable
    public static Integer toIntOrNull(@Nullable String str) {
        if (str == null) {
            return null;
        }

        try {
            return Integer.parseInt(str.trim());
        } catch (final NumberFormatException err) {
            return null;
        }
    }

    /**
     * @param str The string to parse as a positive integer, may be {@code null}.
     * @return The string parsed as a positive integer or {@code null} if the string is not a valid
     * representation of a positive number.
     */
    @Nullable
    public static Integer toPositiveIntOrNull(@Nullable String str) {
        final Integer value = NumberUtil.toIntOrNull(str);
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    /**
     * @param str The string to parse as an integer between {@code min} and {@code max}, may be {@code null}.
     * @param min The minimum value, inclusive.
     * @param max The maximum value, inclusive.
     * @return The string parsed as an integer or {@code null} if the string is not a valid representation of a positive
     * integer or is not between {@code min} and {@code max}.
     */
    @Nullable
    public static Integer toIntBetweenOrNull(@Nullable String str, int min, int max) {
        final Integer value = NumberUtil.toIntOrNull(str);
        if (value == null || !NumberUtil.isBetween(value, min, max)) {
            return null;
        }
        return value;
    }

    /**
     * @param str The string to parse as a long number, may be {@code null}.
     * @return The string parsed as a long number or {@code null} if the string is not a valid representation of a
     * number.
     */
    @Nullable
    public static Long toLongOrNull(@Nullable String str) {
        if (str == null) {
            return null;
        }

        try {
            return Long.parseLong(str.trim());
        } catch (final NumberFormatException err) {
            return null;
        }
    }

    /**
     * @param str The string to parse as a positive long number, may be {@code null}.
     * @return The string parsed as a positive long number or {@code null} if the string is not a valid representation
     * of a number.
     */
    @Nullable
    public static Long toPositiveLongOrNull(@Nullable String str) {
        final Long value = NumberUtil.toLongOrNull(str);
        if (value == null || value <= 0) {
            return null;
        }
        return value;
    }

    /**
     * @param str The string to check, may be {@code null}.
     * @return {@code true} if the string is a valid representation of a positive long number, {@code false} otherwise.
     */
    public static boolean isPositiveLong(@Nullable String str) {
        return NumberUtil.toPositiveLongOrNull(str) != null;
    }

    /**
     * @param num The long to truncate between {@code min} and {@code max}.
     * @param min The minimum value, inclusive.
     * @param max The maximum value, inclusive.
     * @return The long number truncated between {@code min} and {@code max}.
     */
    public static long truncateBetween(long num, long min, long max) {
        return Math.max(min, Math.min(num, max));
    }

    /**
     * @param num The double number to check.
     * @param min The minimum value, inclusive.
     * @param max The maximum value, inclusive.
     * @return {@code true} if {@code num} is between {@code min} and {@code max}, {@code false} otherwise.
     */
    public static boolean isBetween(double num, double min, double max) {
        return num >= min && num <= max;
    }

}
