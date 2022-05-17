package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableType;

import java.util.Map;
import java.util.function.Consumer;

public interface Fetcher<E> {

    Class<E> getJavaClass();

    ImmutableType getImmutableType();

    Map<String, Field> getFieldMap();

    @NewChain
    Fetcher<E> addSelectable();

    @NewChain
    Fetcher<E> addScalars();

    @NewChain
    Fetcher<E> add(String prop);

    @NewChain
    Fetcher<E> remove(String prop);

    @NewChain
    Fetcher<E> add(
            String prop,
            Fetcher<?> childFetcher
    );

    @NewChain
    Fetcher<E> add(
            String prop,
            Consumer<? extends Loader> loaderBlock,
            Fetcher<?> childFetcher
    );
}
