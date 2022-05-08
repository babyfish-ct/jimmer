package org.babyfish.jimmer.sql.runtime;

import java.util.Objects;

public class DbNull {

    private Class<?> type;

    DbNull(Class<?> type) {
        this.type = type;
    }

    public Class<?> getType() {
        return type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DbNull dbNull = (DbNull) o;
        return type.equals(dbNull.type);
    }

    @Override
    public String toString() {
        return "DbNull{" +
                "type=" + type +
                '}';
    }
}
