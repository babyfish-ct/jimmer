package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Loader;
import org.babyfish.jimmer.sql.fetcher.RecursiveListLoader;
import org.babyfish.jimmer.sql.fetcher.RecursiveLoader;

class LoaderImpl implements RecursiveListLoader {

    private ImmutableProp prop;

    private Fetcher<?> childFetcher;

    private int batchSize;

    private int limit = Integer.MAX_VALUE;

    private int depth = 1;

    LoaderImpl(ImmutableProp prop, Fetcher<?> childFetcher) {
        this.prop = prop;
        this.childFetcher = childFetcher;
    }

    @Override
    public RecursiveListLoader batch(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("batchSize cannot be less than 0");
        }
        batchSize = size;
        return this;
    }

    @Override
    public RecursiveListLoader limit(int limit) {
        if (!prop.isEntityList()) {
            throw new IllegalArgumentException(
                    "Cannot set limit because current property \"" +
                            prop +
                            "\" is not list property"
            );
        }
        return this;
    }

    @Override
    public RecursiveListLoader depth(int depth) {
        if (!prop.getDeclaringType().getJavaClass().isAssignableFrom(prop.getTargetType().getJavaClass())) {
            throw new IllegalArgumentException(
                    "Cannot set depth because current property \"" +
                            prop +
                            "\" is not recursive property"
            );
        }
        if (depth < 1) {
            throw new IllegalArgumentException("depth cannot be less than 1");
        }
        this.depth = depth;
        return this;
    }

    @Override
    public RecursiveListLoader recursive() {
        return depth(Integer.MAX_VALUE);
    }

    Fetcher<?> getChildFetcher() {
        return childFetcher;
    }

    int getBatchSize() {
        return batchSize;
    }

    int getLimit() {
        return limit;
    }

    int getDepth() {
        return depth;
    }
}
