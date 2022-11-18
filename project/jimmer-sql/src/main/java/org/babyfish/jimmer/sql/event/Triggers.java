package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.event.AssociationListener;
import org.babyfish.jimmer.sql.event.EntityListener;

import java.sql.Connection;

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

    default void fireEntityTableChange(Object oldRow, Object newRow, Connection con) {
        fireEntityTableChange(oldRow, newRow, con, null);
    }

    void fireEntityTableChange(Object oldRow, Object newRow, Connection con, Object reason);

    default void fireMiddleTableDelete(ImmutableProp prop, Object sourceId, Object targetId, Connection con) {
        fireMiddleTableDelete(prop, sourceId, targetId, con, null);
    }

    void fireMiddleTableDelete(ImmutableProp prop, Object sourceId, Object targetId, Connection con, Object reason);

    default void fireMiddleTableInsert(ImmutableProp prop, Object sourceId, Object targetId, Connection con) {
        fireMiddleTableInsert(prop, sourceId, targetId, con, null);
    }

    void fireMiddleTableInsert(ImmutableProp prop, Object sourceId, Object targetId, Connection con, Object reason);

    default void fireAssociationEvict(ImmutableProp prop, Object sourceId) {
        fireAssociationEvict(prop, sourceId, null);
    }

    void fireAssociationEvict(ImmutableProp prop, Object sourceId, Object reason);
}
