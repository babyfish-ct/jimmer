package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.ast.table.Props;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;

import java.util.NavigableMap;

public interface CacheableFilter<P extends Props> extends Filter<P> {

    NavigableMap<String, Object> getParameters();

    boolean isAffectedBy(EntityEvent<?> e);

    default boolean isAffectedBy(AssociationEvent e) {
        return false;
    }
}
