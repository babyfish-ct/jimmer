package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Loader;

class LoaderImpl implements Loader {

    private Fetcher<?> childFetcher;

    private int batchSize;

    private int limit = Integer.MAX_VALUE;

    LoaderImpl(Fetcher<?> childFetcher) {
        this.childFetcher = childFetcher;
    }

    @Override
    public Loader batch(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("batchSize cannot be less than 0");
        }
        batchSize = size;
        return this;
    }

    @Override
    public Loader limit(int limit) {
        return this;
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
        return 1;
    }
}
