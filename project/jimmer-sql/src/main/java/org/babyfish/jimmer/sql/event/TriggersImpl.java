package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.Triggers;
import org.babyfish.jimmer.sql.meta.MiddleTable;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TriggersImpl implements Triggers {

    private final ConcurrentMap<ImmutableType, CopyOnWriteArrayList<EntityListener<ImmutableSpi>>> entityTableListenerMultiMap =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<ImmutableProp, CopyOnWriteArrayList<MiddleTableListener>> middleTableListenerMultiMap =
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
        ImmutableProp primaryAssociationProp = Utils.primaryAssociationProp(prop);
        if (primaryAssociationProp.getStorage() instanceof MiddleTable) {
            addMiddleTableListener(primaryAssociationProp, new MiddleTableAssociationListenerProxy(prop, listener));
        } else {
            addEntityListener(primaryAssociationProp.getDeclaringType(), new ForeignKeyAssociationListenerProxy(prop, listener));
        }
    }

    @Override
    public void removeAssociationListener(ImmutableProp prop, AssociationListener listener) {
        ImmutableProp primaryAssociationProp = Utils.primaryAssociationProp(prop);
        if (primaryAssociationProp.getStorage() instanceof MiddleTable) {
            removeMiddleTableListener(primaryAssociationProp, new MiddleTableAssociationListenerProxy(prop, listener));
        } else {
            removeEntityListener(primaryAssociationProp.getDeclaringType(), new ForeignKeyAssociationListenerProxy(prop, listener));
        }
    }

    public boolean hasListeners(ImmutableType type) {
        List<EntityListener<ImmutableSpi>> listeners = entityTableListenerMultiMap.get(type);
        return listeners != null && !listeners.isEmpty();
    }

    public boolean hasListeners(TypedProp<?, ?> prop) {
        return hasListeners(prop.unwrap());
    }

    public boolean hasListeners(ImmutableProp prop) {
        ImmutableProp primaryAssociationProp = Utils.primaryAssociationProp(prop);
        if (primaryAssociationProp.getStorage() instanceof MiddleTable) {
            List<MiddleTableListener> listeners =
                    middleTableListenerMultiMap.get(primaryAssociationProp);
            return listeners != null && !listeners.isEmpty();
        }
        List<EntityListener<ImmutableSpi>> listeners =
                entityTableListenerMultiMap.get(primaryAssociationProp.getDeclaringType());
        return listeners != null && !listeners.isEmpty();
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

    @Override
    public void fireEntityTableChange(Object oldRow, Object newRow, Object reason) {
        if (oldRow == null && newRow == null) {
            return;
        }
        if (oldRow != null && !(oldRow instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("oldRow must be immutable");
        }
        if (newRow != null && !(newRow instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("newRow must be immutable");
        }
        EntityEvent<ImmutableSpi> event = new EntityEvent<>((ImmutableSpi)oldRow, (ImmutableSpi) newRow, reason);
        List<EntityListener<ImmutableSpi>> listeners =
                entityTableListenerMultiMap.get(event.getImmutableType());
        if (listeners != null && !listeners.isEmpty()) {
            Throwable throwable = null;
            for (EntityListener<ImmutableSpi> listener : listeners) {
                try {
                    listener.onChange(event);
                } catch (RuntimeException | Error ex) {
                    if (throwable == null) {
                        throwable =ex;
                    }
                }
            }
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            }
            if (throwable != null) {
                throw (Error)throwable;
            }
        }
    }

    @Override
    public void fireMiddleTableDelete(ImmutableProp prop, Object sourceId, Object targetId, Object reason) {
        ImmutableProp primaryAssociationProp = Utils.primaryAssociationProp(prop);
        List<MiddleTableListener> listeners = middleTableListenerMultiMap.get(primaryAssociationProp);
        if (listeners != null && !listeners.isEmpty()) {
            Throwable throwable = null;
            if (prop == primaryAssociationProp) {
                for (MiddleTableListener listener : listeners) {
                    try {
                        listener.delete(sourceId, targetId, reason);
                    } catch (RuntimeException | Error ex) {
                        if (throwable == null) {
                            throwable =ex;
                        }
                    }
                }
            } else {
                for (MiddleTableListener listener : listeners) {
                    try {
                        listener.delete(targetId, sourceId, reason);
                    } catch (RuntimeException | Error ex) {
                        if (throwable == null) {
                            throwable =ex;
                        }
                    }
                }
            }
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            }
            if (throwable != null) {
                throw (Error)throwable;
            }
        }
    }

    @Override
    public void fireMiddleTableInsert(ImmutableProp prop, Object sourceId, Object targetId, Object reason) {
        ImmutableProp primaryAssociationProp = Utils.primaryAssociationProp(prop);
        List<MiddleTableListener> listeners = middleTableListenerMultiMap.get(primaryAssociationProp);
        if (listeners != null && !listeners.isEmpty()) {
            Throwable throwable = null;
            if (prop == primaryAssociationProp) {
                for (MiddleTableListener listener : listeners) {
                    try {
                        listener.insert(sourceId, targetId, reason);
                    } catch (RuntimeException | Error ex) {
                        if (throwable == null) {
                            throwable =ex;
                        }
                    }
                }
            } else {
                for (MiddleTableListener listener : listeners) {
                    try {
                        listener.insert(targetId, sourceId, reason);
                    } catch (RuntimeException | Error ex) {
                        if (throwable == null) {
                            throwable =ex;
                        }
                    }
                }
            }
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            }
            if (throwable != null) {
                throw (Error)throwable;
            }
        }
    }
}
