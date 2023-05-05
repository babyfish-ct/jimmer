package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;
import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;

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

    private final boolean implicit;

    FieldImpl(
            ImmutableType entityType,
            ImmutableProp prop,
            FieldFilter<?> filter,
            int batchSize,
            int limit,
            int offset,
            RecursionStrategy<?> recursionStrategy,
            FetcherImpl<?> childFetcher,
            boolean implicit
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
        this.implicit = implicit;
    }

    FieldImpl(
            FieldImpl base,
            FetcherImpl<?> childFetcher
    ) {
        this.entityType = base.entityType;
        this.prop = base.prop;
        this.filter = base.filter;
        this.batchSize = base.batchSize;
        this.limit = base.limit;
        this.offset = base.offset;
        this.recursionStrategy = base.recursionStrategy;
        this.implicit = base.implicit;
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
    public boolean isImplicit() {
        return implicit;
    }

    @Override
    public String toString() {
        FetcherWriter writer = new FetcherWriter();
        writer.write(this);
        return writer.toString();
    }

    private boolean determineIsSimpleField() {
        if (prop.isColumnDefinition()) {
            return childFetcher == null || childFetcher.getFieldMap().size() == 1;
        }
        if (prop.getSqlTemplate() instanceof FormulaTemplate) {
            return true;
        }
        return false;
    }
}
