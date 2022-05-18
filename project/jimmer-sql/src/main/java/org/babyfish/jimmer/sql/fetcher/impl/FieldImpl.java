package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.meta.Column;

import java.util.StringJoiner;

class FieldImpl implements Field {

    private ImmutableProp prop;

    private int batchSize;

    private int limit;

    private int depth;

    private FetcherImpl<?> childFetcher;

    private boolean isSimpleField;

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
        this.isSimpleField = determineIsSimpleField();
    }

    @Override
    public ImmutableProp getProp() {
        return prop;
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
        return childFetcher;
    }

    @Override
    public boolean isSimpleField() {
        return isSimpleField;
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
        if (childFetcher == null) {
            return prop.getName() + joiner;
        }
        return prop.getName() + joiner + childFetcher.toString(false);
    }

    private boolean determineIsSimpleField() {
        if (prop.getStorage() instanceof Column) {
            return childFetcher == null || childFetcher.getFieldMap().size() == 1;
        }
        return false;
    }
}
