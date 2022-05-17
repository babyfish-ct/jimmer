package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.RecursiveLoader;

class RecursiveLoaderImpl extends LoaderImpl implements RecursiveLoader {

    private int depth;

    public RecursiveLoaderImpl(Fetcher<?> childFetcher) {
        super(childFetcher);
    }

    @Override
    public RecursiveLoader depth(int depth) {
        if (depth < 1) {
            throw new IllegalArgumentException("depth cannot be less than 1");
        }
        this.depth = depth;
        return this;
    }

    @Override
    public RecursiveLoader recursive() {
        return depth(Integer.MAX_VALUE);
    }

    @Override
    public RecursiveLoader batch(int size) {
        return (RecursiveLoader) super.batch(size);
    }

    @Override
    public RecursiveLoader limit(int limit) {
        return (RecursiveLoader) super.limit(limit);
    }

    @Override
    int getDepth() {
        return depth;
    }
}
