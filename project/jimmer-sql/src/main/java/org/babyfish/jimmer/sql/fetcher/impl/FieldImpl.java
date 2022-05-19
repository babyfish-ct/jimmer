package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;
import org.babyfish.jimmer.sql.meta.Column;

import java.util.StringJoiner;
import java.util.function.BiConsumer;

class FieldImpl implements Field {

    private ImmutableProp prop;

    private int batchSize;

    private int limit;

    private RecursionStrategy<?> recursionStrategy;

    private FetcherImpl<?> childFetcher;

    private boolean isSimpleField;

    FieldImpl(
            ImmutableProp prop,
            int batchSize,
            int limit,
            RecursionStrategy<?> recursionStrategy,
            FetcherImpl<?> childFetcher
    ) {
        this.prop = prop;
        this.batchSize = batchSize;
        this.limit = limit;
        this.recursionStrategy = recursionStrategy;
        this.childFetcher = childFetcher;
        this.isSimpleField = determineIsSimpleField();
    }

    @Override
    public ImmutableProp getProp() {
        return prop;
    }

    @Override
    public int getBatchSize() {
        return batchSize;
    }

    @Override
    public int getLimit() {
        return limit;
    }

    @Override
    public RecursionStrategy<?> getRecursionStrategy() {
        return recursionStrategy;
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
        if (recursionStrategy instanceof DefaultRecursionStrategy<?>) {
            int depth = ((DefaultRecursionStrategy<?>) recursionStrategy).getDepth();
            if (depth == Integer.MAX_VALUE) {
                joiner.add("recursive: true");
            } else if (depth > 1) {
                joiner.add("depth: " + depth);
            }
        } else if (recursionStrategy != null) {
            joiner.add("recursive: <java-code>");
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
