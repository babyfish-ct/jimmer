package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.fetcher.Filter;
import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;
import org.babyfish.jimmer.sql.fetcher.RecursiveListLoader;

class LoaderImpl<E, T extends Table<E>> implements RecursiveListLoader<E, T> {

    private ImmutableProp prop;

    private FetcherImpl<?> childFetcher;

    private Filter<E, T> filter;

    private int batchSize;

    private int limit = Integer.MAX_VALUE;

    private int offset = 0;

    private RecursionStrategy<E> recursionStrategy;

    LoaderImpl(ImmutableProp prop, FetcherImpl<?> childFetcher) {
        this.prop = prop;
        this.childFetcher = childFetcher;
    }

    @Override
    public RecursiveListLoader<E, T> filter(Filter<E, T> filter) {
        if (filter != null && prop.isReference() && !prop.isNullable()) {
            throw new IllegalArgumentException(
                    "Cannot set filter for non-null one-to-one/many-to-one property \"" + prop + "\""
            );
        }
        this.filter = filter;
        return this;
    }

    @Override
    public RecursiveListLoader<E, T> batch(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("batchSize cannot be less than 0");
        }
        batchSize = size;
        return this;
    }

    @Override
    public RecursiveListLoader<E, T> limit(int limit, int offset) {
        if (!prop.isEntityList()) {
            throw new IllegalArgumentException(
                    "Cannot set limit because current property \"" +
                            prop +
                            "\" is not list property"
            );
        }
        if (limit < 0) {
            throw new IllegalArgumentException("'limit' can not be less than 0");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("'offset' can not be less than 0");
        }
        if (limit > Integer.MAX_VALUE - offset) {
            throw new IllegalArgumentException("'limit' > Int.MAX_VALUE - offset");
        }
        this.limit = limit;
        this.offset = offset;
        return this;
    }

    @Override
    public RecursiveListLoader<E, T> depth(int depth) {
        return recursive(DefaultRecursionStrategy.of(depth));
    }

    @Override
    public RecursiveListLoader<E, T> recursive() {
        return recursive(DefaultRecursionStrategy.of(Integer.MAX_VALUE));
    }

    @Override
    public RecursiveListLoader<E, T> recursive(RecursionStrategy<E> strategy) {
        if (!prop.getDeclaringType().getJavaClass().isAssignableFrom(prop.getTargetType().getJavaClass())) {
            throw new IllegalArgumentException(
                    "Cannot set recursive strategy because current property \"" +
                            prop +
                            "\" is not recursive property"
            );
        }
        this.recursionStrategy = strategy;
        return this;
    }

    ImmutableProp getProp() {
        return prop;
    }

    FetcherImpl<?> getChildFetcher() {
        return childFetcher;
    }

    Filter<E, T> getFilter() {
        return filter;
    }

    int getBatchSize() {
        return batchSize;
    }

    int getLimit() {
        return limit;
    }

    int getOffset() {
        return offset;
    }

    RecursionStrategy<E> getRecursionStrategy() {
        return recursionStrategy;
    }
}
