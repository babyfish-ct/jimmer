package org.babyfish.jimmer.sql.fetcher.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.fetcher.Fetcher;
import org.babyfish.jimmer.sql.fetcher.Field;
import org.babyfish.jimmer.sql.fetcher.FieldFilter;
import org.babyfish.jimmer.sql.fetcher.RecursionStrategy;
import org.babyfish.jimmer.sql.meta.FormulaTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

class FieldImpl implements Field {

    private final ImmutableType entityType;

    private final ImmutableProp prop;

    private final FieldFilter<?> filter;

    private final int batchSize;

    private final int limit;

    private final int offset;

    @Nullable
    private final RecursionStrategy<?> recursionStrategy;

    @Nullable
    private final FetcherImpl<?> childFetcher;

    private final boolean isSimpleField;

    private final boolean implicit;

    private final boolean rawId;

    private Fetcher<?> recursiveParent;

    private Field recursionResolved;

    FieldImpl(
            ImmutableType entityType,
            ImmutableProp prop,
            FieldFilter<?> filter,
            int batchSize,
            int limit,
            int offset,
            RecursionStrategy<?> recursionStrategy,
            FetcherImpl<?> childFetcher,
            boolean implicit,
            boolean rawId
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
        this.rawId = rawId;
    }

    FieldImpl(
            FieldImpl base,
            @Nullable
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
        this.rawId = base.rawId;
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

    @Nullable
    @Override
    public RecursionStrategy<?> getRecursionStrategy() {
        return recursionStrategy;
    }

    @Override
    public Fetcher<?> getChildFetcher() {
        return childFetcher;
    }

    @Override
    public @Nullable Fetcher<?> getChildFetcher(boolean resolveRecursion) {
        if (resolveRecursion && recursiveParent != null) {
            return recursiveParent;
        }
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
    public boolean isRawId() {
        return rawId;
    }

    @Override
    public Field resolveRecursion() {
        Field rr = this.recursionResolved;
        if (rr == null) {
            if (recursiveParent != null) {
                rr = recursiveParent.getFieldMap().get(prop.getName());
            } else {
                rr = this;
            }
            this.recursionResolved = rr;
        }
        return rr;
    }

    @Override
    public int hashCode() {
        int result = entityType.hashCode();
        result = 31 * result + prop.hashCode();
        result = 31 * result + (filter != null ? filter.hashCode() : 0);
        result = 31 * result + batchSize;
        result = 31 * result + limit;
        result = 31 * result + offset;
        result = 31 * result + (recursionStrategy != null ? recursionStrategy.hashCode() : 0);
        result = 31 * result + (childFetcher != null ? childFetcher.hashCode() : 0);
        result = 31 * result + (isSimpleField ? 1 : 0);
        result = 31 * result + (implicit ? 1 : 0);
        result = 31 * result + (rawId ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldImpl field = (FieldImpl) o;

        if (batchSize != field.batchSize) return false;
        if (limit != field.limit) return false;
        if (offset != field.offset) return false;
        if (isSimpleField != field.isSimpleField) return false;
        if (implicit != field.implicit) return false;
        if (rawId != field.rawId) return false;
        if (!entityType.equals(field.entityType)) return false;
        if (!prop.equals(field.prop)) return false;
        if (!Objects.equals(filter, field.filter)) return false;
        if (!Objects.equals(recursionStrategy, field.recursionStrategy))
            return false;
        return Objects.equals(childFetcher, field.childFetcher);
    }

    @Override
    public String toString() {
        FetcherWriter writer = new FetcherWriter();
        writer.write(this);
        return writer.toString();
    }

    void initializeRecursiveParent(Fetcher<?> recursiveParent) {
        if (this.recursiveParent != null) {
            throw new IllegalStateException("The recursive parent has been set");
        }
        this.recursiveParent = recursiveParent;
    }

    private boolean determineIsSimpleField() {
        if (prop.isColumnDefinition()) {
            return childFetcher == null || (
                    childFetcher.getFieldMap().size() == 1 &&
                            childFetcher.getFieldMap().values().iterator().next().getProp().isId()
            );
        }
        if (prop.getSqlTemplate() instanceof FormulaTemplate) {
            return true;
        }
        return false;
    }
}
