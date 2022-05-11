package org.babyfish.jimmer.sql.dialect;

import java.util.Objects;

public final class UpdateJoin {

    private boolean joinedTableUpdatable;

    private From from;

    public UpdateJoin(boolean joinedTableUpdatable, From from) {
        this.joinedTableUpdatable = joinedTableUpdatable;
        this.from = from;
    }

    public boolean isJoinedTableUpdatable() {
        return joinedTableUpdatable;
    }

    public From getFrom() {
        return from;
    }

    @Override
    public int hashCode() {
        return Objects.hash(joinedTableUpdatable, from);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateJoin that = (UpdateJoin) o;
        return joinedTableUpdatable == that.joinedTableUpdatable && from == that.from;
    }

    @Override
    public String toString() {
        return "UpdateJoin{" +
                "joinedTableUpdatable=" + joinedTableUpdatable +
                ", from=" + from +
                '}';
    }

    public enum From {
        UNNECESSARY,
        AS_ROOT,
        AS_JOIN
    }
}
