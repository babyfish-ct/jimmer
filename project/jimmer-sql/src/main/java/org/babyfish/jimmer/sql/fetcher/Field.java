package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;

public interface Field {

    /**
     * If the declaring type of {@link #getProp()} has id property,
     * the returned type is declaring type of {@link #getProp()},
     * otherwise(Interface decorated by MappedSuperclass and have no id property)
     * the returned type is the type that contain id property.
     * @return
     */
    ImmutableType getEntityType();

    ImmutableProp getProp();

    FieldFilter<?> getFilter();

    int getBatchSize();

    int getLimit();

    int getOffset();

    RecursionStrategy<?> getRecursionStrategy();

    Fetcher<?> getChildFetcher();

    boolean isSimpleField();
}
