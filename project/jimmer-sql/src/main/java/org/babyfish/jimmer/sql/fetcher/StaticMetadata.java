package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.Static;

import java.util.Objects;
import java.util.function.Function;

public final class StaticMetadata<E, S> {

    private final Fetcher<E> fetcher;

    private final Function<E, S> converter;

    public StaticMetadata(Fetcher<E> fetcher, Function<E, S> converter) {
        this.fetcher = Objects.requireNonNull(fetcher, "fetch cannot be null");
        this.converter = Objects.requireNonNull(converter, "converter cannot be null");
    }

    public Fetcher<E> getFetcher() {
        return fetcher;
    }

    public Function<E, S> getConverter() {
        return converter;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fetcher, converter);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StaticMetadata<?, ?> that = (StaticMetadata<?, ?>) o;
        return fetcher.equals(that.fetcher) && converter.equals(that.converter);
    }

    @Override
    public String toString() {
        return "StaticMetadata{" +
                "fetcher=" + fetcher +
                ", converter=" + converter +
                '}';
    }

    public static <E, S extends Static<E>> StaticMetadata<E, S> of(Class<S> staticType) {
        return null;
    }
}
