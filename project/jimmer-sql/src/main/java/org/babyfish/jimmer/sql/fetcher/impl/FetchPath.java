package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;

import java.util.Objects;

public class FetchPath {

    private final FetchPath parent;

    private final ImmutableProp prop;

    private FetchPath(FetchPath parent, ImmutableProp prop) {
        this.parent = parent;
        this.prop = prop;
    }

    public static FetchPath of(FetchPath parent, ImmutableProp prop) {
        if (parent != null && parent.prop == prop) {
            return parent;
        }
        return new FetchPath(parent, prop);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FetchPath fetchPath = (FetchPath) o;

        if (!Objects.equals(parent, fetchPath.parent)) return false;
        return prop.equals(fetchPath.prop);
    }

    @Override
    public int hashCode() {
        int result = parent != null ? parent.hashCode() : 0;
        result = 31 * result + prop.hashCode();
        return result;
    }

    @Override
    public String toString() {
        if (parent != null) {
            return parent.toString() + '.' + prop.getName();
        }
        return prop.getName();
    }
}
