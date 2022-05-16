package org.babyfish.jimmer.sql.association;

import java.util.Objects;

public class Association<S, T> {

    private S source;

    private T target;

    public Association(S source, T target) {
        this.source = source;
        this.target = target;
    }

    public S source() {
        return source;
    }

    public T target() {
        return target;
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, target);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Association<?, ?> that = (Association<?, ?>) o;
        return Objects.equals(source, that.source) && Objects.equals(target, that.target);
    }

    @Override
    public String toString() {
        return "Association{" +
                "source=" + source +
                ", target=" + target +
                '}';
    }
}
