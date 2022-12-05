package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;
import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;

import java.util.StringJoiner;

class FieldImpl implements Field {

    private final ImmutableType entityType;

    private final ImmutableProp prop;

    private final FieldFilter<?> filter;

    private final int batchSize;

    private final int limit;

    private final int offset;

    private final RecursionStrategy<?> recursionStrategy;

    private final FetcherImpl<?> childFetcher;

    private final boolean isSimpleField;

    FieldImpl(
            ImmutableType entityType,
            ImmutableProp prop,
            FieldFilter<?> filter,
            int batchSize,
            int limit,
            int offset,
            RecursionStrategy<?> recursionStrategy,
            FetcherImpl<?> childFetcher
    ) {
        this.entityType = entityType;
        this.prop = prop;
        this.filter = filter;
        this.batchSize = batchSize;
        this.limit = limit;
        this.offset = offset;
        this.recursionStrategy = recursionStrategy;
        this.childFetcher = childFetcher;
        this.isSimpleField = determineIsSimpleField();
    }

    @Override
    public ImmutableType getEntityType() {
        return entityType;
    }

    @Override
    public ImmutableProp getProp() {
        return prop;
    }

    @Override
    public FieldFilter<?> getFilter() {
        return filter;
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
    public int getOffset() {
        return offset;
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
        if (prop.getStorage() instanceof ColumnDefinition) {
            return childFetcher == null || childFetcher.getFieldMap().size() == 1;
        }
        return false;
    }
}
