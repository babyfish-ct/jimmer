package org.babyfish.jimmer.sql.fetcher.spi;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.Loader;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;

import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractTypedFetcher<E> implements Fetcher<E> {

    protected Fetcher<E> raw;

    protected AbstractTypedFetcher(Class<E> type) {
        this.raw = new FetcherImpl<>(type);
    }

    protected AbstractTypedFetcher(Fetcher<E> raw) {
        this.raw = raw;
    }

    @Override
    public Class<E> getJavaClass() {
        return raw.getJavaClass();
    }

    @Override
    public ImmutableType getImmutableType() {
        return raw.getImmutableType();
    }

    @Override
    public Map<String, Field> getFieldMap() {
        return raw.getFieldMap();
    }

    @Override
    public Fetcher<E> addSelectable() {
        return raw.addSelectable();
    }

    @Override
    public Fetcher<E> addScalars() {
        return raw.addScalars();
    }

    @NewChain
    @Override
    public Fetcher<E> add(String prop) {
        return raw.add(prop);
    }

    @NewChain
    @Override
    public Fetcher<E> remove(String prop) {
        return raw.remove(prop);
    }

    @NewChain
    @Override
    public Fetcher<E> add(
            String prop,
            Fetcher<?> childFetcher
    ) {
        return add(prop, null, childFetcher);
    }

    @NewChain
    @Override
    public Fetcher<E> add(
            String prop,
            Consumer<? extends Loader> loaderBlock,
            Fetcher<?> childFetcher
    ) {
        return raw.add(prop, loaderBlock, childFetcher);
    }

    public Fetcher<E> __unwrap() {
        return raw;
    }
}
