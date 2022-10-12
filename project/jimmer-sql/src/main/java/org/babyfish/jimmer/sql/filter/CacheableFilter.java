package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.ast.table.Columns;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;

import java.util.NavigableMap;

public interface CacheableFilter<C extends Columns> extends Filter<C> {

    NavigableMap<String, Object> getParameters();

    boolean isAffectedBy(EntityEvent<?> e);

    default boolean isAffectedBy(AssociationEvent e) {
        return false;
    }
}
