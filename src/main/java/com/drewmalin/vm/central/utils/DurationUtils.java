package com.drewmalin.vm.central.utils;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.StringJoiner;

public class DurationUtils {

    private DurationUtils() {

    }

    public static String toString(final long startMillis) {
        return toString(startMillis, System.currentTimeMillis());
    }

    public static String toString(final long startMillis, final long endMillis) {
        return toString(Duration.of(endMillis - startMillis, ChronoUnit.MILLIS));
    }

    public static String toString(final Duration duration) {

        final var sj = new StringJoiner(", ", "", "");

        final var days = duration.toDaysPart();
        if (days > 0) {
            sj.add(toString(days, "day"));
        }

        final var hours = duration.toHoursPart();
        if (hours > 0) {
            sj.add(toString(hours, "hour"));
        }

        final var minutes = duration.toMinutesPart();
        if (minutes > 0) {
            sj.add(toString(minutes, "minute"));
        }

        final var seconds = duration.toSecondsPart();
        if (seconds > 0) {
            sj.add(toString(seconds, "second"));
        }

        final var millis = duration.toMillisPart();
        if (millis > 0) {
            sj.add(toString(millis, "millisecond"));
        }

        return sj.toString();
    }

    private static String toString(final long amount, final String unit) {
        final var unitString = amount == 1
            ? unit
            : unit + "s";

        return "%d %s".formatted(amount, unitString);
    }
}
