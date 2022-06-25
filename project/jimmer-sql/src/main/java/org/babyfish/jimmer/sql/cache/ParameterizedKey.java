package org.babyfish.jimmer.sql.cache;

import java.util.Objects;

public class ParameterizedKey<K> {

    private final K raw;

    private final CacheFilter filter;

    public ParameterizedKey(K raw, CacheFilter filter) {
        this.raw = raw;
        this.filter = filter;
    }

    public K getRaw() {
        return raw;
    }

    public CacheFilter getFilter() {
        return filter;
    }

    @Override
    public int hashCode() {
        return Objects.hash(raw, filter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterizedKey<?> that = (ParameterizedKey<?>) o;
        return Objects.equals(raw, that.raw) && Objects.equals(filter, that.filter);
    }

    @Override
    public String toString() {
        return "ParameterizedKey{" +
                "raw=" + raw +
                ", filter=" + filter +
                '}';
    }
}
