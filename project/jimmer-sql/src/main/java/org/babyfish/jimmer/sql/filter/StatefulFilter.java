package org.babyfish.jimmer.sql.filter;

import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.event.AssociationEvent;
import org.babyfish.jimmer.sql.event.EntityEvent;

import java.util.NavigableMap;

public interface StatefulFilter<T extends Table<?>> extends Filter<T> {

    default NavigableMap<String, Object> getParameters() {
        throw new UnsupportedOperationException(
                "`" +
                        this.getClass().getName() +
                        "getParameters()` has not been implemented"
        );
    }

    default boolean isAffectedBy(EntityEvent<?> e) {
        throw new UnsupportedOperationException(
                "`" +
                        this.getClass().getName() +
                        "isAffectedBy(EntityEvent)` has not been implemented"
        );
    }

    default boolean isAffectedBy(AssociationEvent e) {
        throw new UnsupportedOperationException(
                "`" +
                        this.getClass().getName() +
                        "isAffectedBy(AssociationEvent)` has not been implemented"
        );
    }
}
