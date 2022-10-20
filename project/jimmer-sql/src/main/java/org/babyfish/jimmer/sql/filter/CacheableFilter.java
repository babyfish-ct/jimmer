package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.event.EntityEvent;

import java.util.SortedMap;

public interface CacheableFilter<P extends Props> extends Filter<P> {

    SortedMap<String, Object> getParameters();

    boolean isAffectedBy(EntityEvent<?> e);
}
