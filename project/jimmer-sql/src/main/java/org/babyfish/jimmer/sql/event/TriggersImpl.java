package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.Triggers;
import org.babyfish.jimmer.sql.meta.MiddleTable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TriggersImpl implements Triggers {

    private ConcurrentMap<ImmutableType, CopyOnWriteArrayList<EntityListener<ImmutableSpi>>> entityTableListenerMultiMap =
            new ConcurrentHashMap<>();

    private ConcurrentMap<ImmutableProp, CopyOnWriteArrayList<MiddleTableListener>> middleTableListenerMultiMap =
            new ConcurrentHashMap<>();

    @Override
    public void addEntityListener(ImmutableType immutableType, EntityListener<ImmutableSpi> listener) {
        entityTableListenerMultiMap
                .computeIfAbsent(
                        immutableType,
                        it -> new CopyOnWriteArrayList<>()
                )
                .add(listener);
    }

    @Override
    public void removeEntityListener(ImmutableType immutableType, EntityListener<ImmutableSpi> listener) {
        entityTableListenerMultiMap
                .computeIfAbsent(
                        immutableType,
                        it -> new CopyOnWriteArrayList<>()
                )
                .remove(listener);
    }

    @Override
    public void addAssociationListener(ImmutableProp prop, AssociationListener listener) {
        if (!prop.isAssociation()) {
            throw new IllegalArgumentException("\"" + prop + "\" is not association property");
        }
        ImmutableProp primaryAssociationProp = Utils.primaryAssociationProp(prop);
        if (primaryAssociationProp.getStorage() instanceof MiddleTable) {
            addMiddleTableListener(primaryAssociationProp, new MiddleTableAssociationListenerProxy(prop, listener));
        } else {
            addEntityListener(primaryAssociationProp.getDeclaringType(), new ForeignKeyAssociationListenerProxy(prop, listener));
        }
    }

    @Override
    public void removeAssociationListener(ImmutableProp prop, AssociationListener listener) {
        if (!prop.isAssociation()) {
            throw new IllegalArgumentException("\"" + prop + "\" is not association property");
        }
        ImmutableProp primaryAssociationProp = Utils.primaryAssociationProp(prop);
        if (primaryAssociationProp.getStorage() instanceof MiddleTable) {
            removeMiddleTableListener(primaryAssociationProp, new MiddleTableAssociationListenerProxy(prop, listener));
        } else {
            removeEntityListener(primaryAssociationProp.getDeclaringType(), new ForeignKeyAssociationListenerProxy(prop, listener));
        }
    }

    public void fireEntityTableChange(ImmutableSpi oldRow, ImmutableSpi newRow) {
        if (oldRow == null && newRow == null) {
            return;
        }
        EntityEvent<ImmutableSpi> event = new EntityEvent<>(oldRow, newRow);
        List<EntityListener<ImmutableSpi>> listeners =
                entityTableListenerMultiMap.get(event.getImmutableType());
        if (listeners != null && !listeners.isEmpty()) {
            for (EntityListener<ImmutableSpi> listener : listeners) {
                listener.onChange(event);
            }
        }
    }

    public void fireMiddleTableDelete(ImmutableProp prop, Object sourceId, Object targetId) {
        ImmutableProp primaryAssociationProp = Utils.primaryAssociationProp(prop);
        List<MiddleTableListener> listeners = middleTableListenerMultiMap.get(primaryAssociationProp);
        if (listeners != null && !listeners.isEmpty()) {
            if (prop == primaryAssociationProp) {
                for (MiddleTableListener listener : listeners) {
                    listener.delete(sourceId, targetId);
                }
            } else {
                for (MiddleTableListener listener : listeners) {
                    listener.delete(targetId, sourceId);
                }
            }
        }
    }

    public void fireMiddleTableInsert(ImmutableProp prop, Object sourceId, Object targetId) {
        ImmutableProp primaryAssociationProp = Utils.primaryAssociationProp(prop);
        List<MiddleTableListener> listeners = middleTableListenerMultiMap.get(primaryAssociationProp);
        if (listeners != null && !listeners.isEmpty()) {
            if (prop == primaryAssociationProp) {
                for (MiddleTableListener listener : listeners) {
                    listener.insert(sourceId, targetId);
                }
            } else {
                for (MiddleTableListener listener : listeners) {
                    listener.insert(targetId, sourceId);
                }
            }
        }
    }

    private void addMiddleTableListener(ImmutableProp primaryAssociationProp, MiddleTableListener listener) {
        middleTableListenerMultiMap
                .computeIfAbsent(
                        primaryAssociationProp,
                        it -> new CopyOnWriteArrayList<>()
                )
                .add(listener);
    }

    private void removeMiddleTableListener(ImmutableProp primaryAssociationProp, MiddleTableListener listener) {
        middleTableListenerMultiMap
                .computeIfAbsent(
                        primaryAssociationProp,
                        it -> new CopyOnWriteArrayList<>()
                )
                .remove(listener);
    }
}
