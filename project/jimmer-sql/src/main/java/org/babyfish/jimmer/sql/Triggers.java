package org.babyfish.jimmer.sql;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.table.Table;
import org.babyfish.jimmer.sql.event.AssociationListener;
import org.babyfish.jimmer.sql.event.EntityListener;

import java.util.function.Function;

public interface Triggers {

    @SuppressWarnings("unchecked")
    default <E> void addEntityListener(Class<E> entityType, EntityListener<E> listener) {
        addEntityListener(ImmutableType.get(entityType), (EntityListener<ImmutableSpi>) listener);
    }

    @SuppressWarnings("unchecked")
    default <E> void removeEntityListener(Class<E> entityType, EntityListener<E> listener) {
        removeEntityListener(ImmutableType.get(entityType), (EntityListener<ImmutableSpi>) listener);
    }

    void addEntityListener(ImmutableType immutableType, EntityListener<ImmutableSpi> listener);

    void removeEntityListener(ImmutableType immutableType, EntityListener<ImmutableSpi> listener);

    default <ST extends Table<?>> void addAssociationListener(
            Class<ST> sourceTableType,
            Function<ST, ? extends Table<?>> targetTableGetter,
            AssociationListener listener
    ) {
        addAssociationListener(
                ImmutableProps.join(sourceTableType, targetTableGetter),
                listener
        );
    }

    default <ST extends Table<?>> void removeAssociationListener(
            Class<ST> sourceTableType,
            Function<ST, ? extends Table<?>> targetTableGetter,
            AssociationListener listener
    ) {
        removeAssociationListener(
                ImmutableProps.join(sourceTableType, targetTableGetter),
                listener
        );
    }

    void addAssociationListener(ImmutableProp prop, AssociationListener listener);

    void removeAssociationListener(ImmutableProp prop, AssociationListener listener);

    boolean hasListeners(ImmutableType type);

    boolean hasListeners(ImmutableProp prop);

    void fireEntityTableChange(ImmutableSpi oldRow, ImmutableSpi newRow);

    void fireMiddleTableDelete(ImmutableProp prop, Object sourceId, Object targetId);
}
