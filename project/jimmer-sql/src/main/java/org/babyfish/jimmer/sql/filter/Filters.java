package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.sql.ast.table.Props;

public interface Filters {

    Filter<Props> getFilter(Class<?> type);

    Filter<Props> getFilter(ImmutableType type);

    Filter<Props> getTargetFilter(ImmutableProp prop);

    Filter<Props> getTargetFilter(TypedProp.Association<?, ?> prop);

    CacheableFilter<Props> getCacheableFilter(Class<?> type);

    CacheableFilter<Props> getCacheableFilter(ImmutableType type);

    CacheableFilter<Props> getCacheableTargetFilter(ImmutableProp prop);

    CacheableFilter<Props> getCacheableTargetFilter(TypedProp.Association<?, ?> prop);
}
