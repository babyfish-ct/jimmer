package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.event.AssociationListener;
import org.babyfish.jimmer.sql.event.EntityListener;

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

    default void addAssociationListener(TypedProp<?, ?> prop, AssociationListener listener) {
        addAssociationListener(prop.unwrap(), listener);
    }

    void addAssociationListener(ImmutableProp prop, AssociationListener listener);

    default void removeAssociationListener(TypedProp<?, ?> prop, AssociationListener listener) {
        removeAssociationListener(prop.unwrap(), listener);
    }

    void removeAssociationListener(ImmutableProp prop, AssociationListener listener);

    default void fireEntityTableChange(Object oldRow, Object newRow) {
        fireEntityTableChange(oldRow, newRow, null);
    }

    void fireEntityTableChange(Object oldRow, Object newRow, Object reason);

    default void fireMiddleTableDelete(ImmutableProp prop, Object sourceId, Object targetId) {
        fireMiddleTableDelete(prop, sourceId, targetId, null);
    }

    void fireMiddleTableDelete(ImmutableProp prop, Object sourceId, Object targetId, Object reason);

    default void fireMiddleTableInsert(ImmutableProp prop, Object sourceId, Object targetId) {
        fireMiddleTableInsert(prop, sourceId, targetId, null);
    }

    void fireMiddleTableInsert(ImmutableProp prop, Object sourceId, Object targetId, Object reason);

    default void fireAssociationEvict(ImmutableProp prop, Object sourceId) {
        fireAssociationEvict(prop, sourceId, null);
    }

    void fireAssociationEvict(ImmutableProp prop, Object sourceId, Object reason);
}
