package org.babyfish.jimmer.sql.fetcher.spi;

import org.babyfish.jimmer.lang.NewChain;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.FieldConfig;
import org.babyfish.jimmer.sql.fetcher.impl.FetcherImpl;

import java.util.function.Consumer;

public abstract class AbstractTypedFetcher<E, T extends AbstractTypedFetcher<E, T>> extends FetcherImpl<E> {

    protected AbstractTypedFetcher(Class<E> type, FetcherImpl<E> base) {
        super(type, base);
    }

    protected AbstractTypedFetcher(
            FetcherImpl<E> prev,
            ImmutableProp prop,
            boolean negative
    ) {
        super(prev, prop, negative);
    }

    protected AbstractTypedFetcher(
            FetcherImpl<E> prev,
            ImmutableProp prop,
            FieldConfig<?, ? extends Table<?>> fieldConfig
    ) {
        super(prev, prop, fieldConfig);
    }

    @SuppressWarnings("unchecked")
    @Override
    public T allTableFields() {
        return (T) super.allTableFields();
    }

    @SuppressWarnings("unchecked")
    @Override
    public T allScalarFields() {
        return (T) super.allScalarFields();
    }

    @NewChain
    @SuppressWarnings("unchecked")
    @Override
    public T add(String prop) {
        return (T) super.add(prop);
    }

    @NewChain
    @SuppressWarnings("unchecked")
    @Override
    public T remove(String prop) {
        return (T) super.remove(prop);
    }

    @NewChain
    @SuppressWarnings("unchecked")
    @Override
    public T add(
            String prop,
            Fetcher<?> childFetcher
    ) {
        return (T) super.add(prop, childFetcher);
    }

    @NewChain
    @SuppressWarnings("unchecked")
    @Override
    public T add(
            String prop,
            Fetcher<?> childFetcher,
            Consumer<? extends FieldConfig<?, ? extends Table<?>>> loaderBlock
    ) {
        return (T) super.add(prop, childFetcher, loaderBlock);
    }

    @Override
    protected abstract T createChildFetcher(ImmutableProp prop, boolean negative);

    @Override
    protected abstract T createChildFetcher(ImmutableProp prop, FieldConfig<?, ? extends Table<?>> fieldConfig);
}
