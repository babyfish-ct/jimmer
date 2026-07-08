package org.babyfish.jimmer.sql.dialect;

import java.util.Objects;

public final class DeleteJoin {

    private final From from;

    public DeleteJoin(From from) {
        this.from = from;
    }

    public From getFrom() {
        return from;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeleteJoin that = (DeleteJoin) o;
        return from == that.from;
    }

    @Override
    public String toString() {
        return "DeleteJoin{" +
                "from=" + from +
                '}';
    }

    public enum From {
        AS_JOIN,
        AS_USING
    }
}
