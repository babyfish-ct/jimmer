package org.babyfish.jimmer.sql.fetcher;

import org.babyfish.jimmer.meta.ImmutableProp;

public interface Field {

    ImmutableProp getProp();

    Filter<?,?> getFilter();

    int getBatchSize();

    int getLimit();

    int getOffset();

    RecursionStrategy<?> getRecursionStrategy();

    Fetcher<?> getChildFetcher();

    boolean isSimpleField();
}
