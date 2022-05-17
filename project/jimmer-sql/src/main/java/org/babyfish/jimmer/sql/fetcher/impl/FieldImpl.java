package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;

import java.util.StringJoiner;

class FieldImpl implements Field {

    private ImmutableProp prop;

    private int batchSize;

    private int limit;

    private int depth;

    private FetcherImpl<?> childFetcher;

    FieldImpl(
            ImmutableProp prop,
            int batchSize,
            int limit,
            int depth,
            FetcherImpl<?> childFetcher
    ) {
        this.prop = prop;
        this.batchSize = batchSize;
        this.limit = limit;
        this.depth = depth;
        this.childFetcher = childFetcher;
    }

    @Override
    public ImmutableProp getProp() {
        return null;
    }

    @Override
    public int getBatchSize() {
        return 0;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public int getDepth() {
        return 0;
    }

    @Override
    public Fetcher<?> getChildFetcher() {
        return null;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "(", ")").setEmptyValue("");
        if (batchSize != 0) {
            joiner.add("batchSize: " + batchSize);
        }
        if (limit != Integer.MAX_VALUE) {
            joiner.add("limit: " + limit);
        }
        if (depth == Integer.MAX_VALUE) {
            joiner.add("recursive: true");
        } else if (depth > 1) {
            joiner.add("depth: " + depth);
        }
        if (childFetcher != null) {
            joiner.add("childFetcher: " + childFetcher.toString(false));
        }
        return prop.getName() + joiner;
    }
}
