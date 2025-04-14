package org.babyfish.jimmer.sql.ast;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public enum SqlTimeUnit {

    NANOSECONDS,
    MICROSECONDS,
    MILLISECONDS,
    SECONDS,
    MINUTES,
    HOURS,
    DAYS,
    WEEKS,
    MONTHS,
    QUARTERS,
    YEARS,
    DECADES,
    CENTURIES;

    public Optional<TimeUnit> toJdkTimeUnit() {
        switch (this) {
            case NANOSECONDS:
                return Optional.of(TimeUnit.NANOSECONDS);
            case MICROSECONDS:
                return Optional.of(TimeUnit.MICROSECONDS);
            case MILLISECONDS:
                return Optional.of(TimeUnit.MILLISECONDS);
            case SECONDS:
                return Optional.of(TimeUnit.SECONDS);
            case MINUTES:
                return Optional.of(TimeUnit.MINUTES);
            case HOURS:
                return Optional.of(TimeUnit.HOURS);
            case DAYS:
                return Optional.of(TimeUnit.DAYS);
            default:
                return Optional.empty();
        }
    }

    public static SqlTimeUnit fromJdkTimeUnit(TimeUnit jdkTimeUnit) {
        switch (jdkTimeUnit) {
            case NANOSECONDS:
                return NANOSECONDS;
            case MICROSECONDS:
                return MICROSECONDS;
            case MILLISECONDS:
                return MILLISECONDS;
            case SECONDS:
                return SECONDS;
            case MINUTES:
                return MINUTES;
            case HOURS:
                return HOURS;
            case DAYS:
                return DAYS;
            default:
                throw new IllegalArgumentException("Unsupported jdk timeunit");
        }
    }
}
