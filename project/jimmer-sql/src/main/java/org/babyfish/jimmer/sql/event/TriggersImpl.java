package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TriggersImpl implements Triggers {

    private final CopyOnWriteArrayList<EntityListener<ImmutableSpi>> globalEntityListeners =
            new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<AssociationListener> globalAssociationListeners =
            new CopyOnWriteArrayList<>();

    private final ConcurrentMap<ImmutableType, CopyOnWriteArrayList<EntityListener<ImmutableSpi>>> entityTableListenerMultiMap =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<ImmutableProp, CopyOnWriteArrayList<AssociationListener>> associationListenerMultiMap =
            new ConcurrentHashMap<>();

    @Override
    public void addEntityListener(EntityListener<?> listener) {
        addEntityListener((ImmutableType) null, listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void addEntityListener(ImmutableType immutableType, EntityListener<?> listener) {
        if (listener != null) {
            if (immutableType == null) {
                globalEntityListeners.add((EntityListener<ImmutableSpi>) listener);
            } else {
                entityTableListenerMultiMap
                        .computeIfAbsent(
                                immutableType,
                                it -> new CopyOnWriteArrayList<>()
                        )
                        .add((EntityListener<ImmutableSpi>) listener);
            }
        }
    }

    @Override
    public void removeEntityListener(EntityListener<?> listener) {
        removeEntityListener((ImmutableType)null, listener);
    }

    @Override
    public void removeEntityListener(ImmutableType immutableType, EntityListener<?> listener) {
        if (listener != null) {
            if (immutableType == null) {
                globalEntityListeners.remove(listener);
            } else {
                entityTableListenerMultiMap
                        .computeIfAbsent(
                                immutableType,
                                it -> new CopyOnWriteArrayList<>()
                        )
                        .remove(listener);
            }
        }
    }

    @Override
    public void addAssociationListener(AssociationListener listener) {
        addAssociationListener((ImmutableProp) null, listener);
    }

    @Override
    public void addAssociationListener(ImmutableProp prop, AssociationListener listener) {
        if (prop != null && !prop.getDeclaringType().isEntity()) {
            throw new IllegalArgumentException("\"" + prop + "\" is not declared in entity");
        }
        if (listener != null) {
            if (prop == null) {
                globalAssociationListeners.add(listener);
            } else {
                associationListenerMultiMap
                        .computeIfAbsent(
                                prop,
                                it -> new CopyOnWriteArrayList<>()
                        )
                        .add(listener);
            }
        }
    }

    @Override
    public void removeAssociationListener(AssociationListener listener) {
        removeAssociationListener((ImmutableProp) null, listener);
    }

    @Override
    public void removeAssociationListener(ImmutableProp prop, AssociationListener listener) {
        if (prop != null && !prop.getDeclaringType().isEntity()) {
            throw new IllegalArgumentException("\"" + prop + "\" is not declared in entity");
        }
        if (listener != null) {
            if (prop == null) {
                globalAssociationListeners.remove(listener);
            } else {
                associationListenerMultiMap
                        .computeIfAbsent(
                                prop,
                                it -> new CopyOnWriteArrayList<>()
                        )
                        .remove(listener);
            }
        }
    }

    @Override
    public void fireEntityTableChange(Object oldRow, Object newRow, Connection con, Object reason) {
        if (oldRow == null && newRow == null) {
            return;
        }
        if (oldRow != null && !(oldRow instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("oldRow must be immutable");
        }
        if (newRow != null && !(newRow instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("newRow must be immutable");
        }
        EntityEvent<ImmutableSpi> event = new EntityEvent<>((ImmutableSpi)oldRow, (ImmutableSpi) newRow, con, reason);
        List<EntityListener<ImmutableSpi>> listeners = entityListeners(event.getImmutableType());
        Throwable throwable = null;
        if (!listeners.isEmpty()) {
            for (EntityListener<ImmutableSpi> listener : listeners) {
                try {
                    listener.onChange(event);
                } catch (RuntimeException | Error ex) {
                    if (throwable == null) {
                        throwable =ex;
                    }
                }
            }
        }
        ImmutableType type = event.getImmutableType();
        for (ImmutableProp prop : type.getProps().values()) {
            if (prop.getStorage() instanceof ColumnDefinition && prop.isAssociation(TargetLevel.PERSISTENT)) {
                ChangedRef<Object> changedRef = event.getChangedFieldRef(prop);
                if (changedRef != null) {
                    ChangedRef<Object> fkRef = changedRef.toIdRef();
                    throwable = fireForeignKeyChange(
                            prop,
                            event.getId(),
                            fkRef.getOldValue(),
                            fkRef.getNewValue(),
                            con,
                            reason,
                            throwable
                    );
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

    private Throwable fireForeignKeyChange(
            ImmutableProp prop,
            Object childId,
            Object oldFk,
            Object newFk,
            Connection con,
            Object reason,
            Throwable throwable
    ) {
        ImmutableProp inverseProp = prop.getOpposite();
        List<AssociationListener> listeners = associationListeners(prop);
        List<AssociationListener> inverseListeners = associationListeners(inverseProp);
        if (!listeners.isEmpty()) {
            AssociationEvent e = new AssociationEvent(prop, childId, oldFk, newFk, con, reason);
            for (AssociationListener listener : listeners) {
                try {
                    listener.onChange(e);
                } catch (RuntimeException | Error ex) {
                    if (throwable == null) {
                        throwable = ex;
                    }
                }
            }
        }
        if (!inverseListeners.isEmpty()) {
            if (oldFk != null) {
                AssociationEvent e = new AssociationEvent(inverseProp, oldFk, childId, null, con, reason);
                for (AssociationListener inverseListener : inverseListeners) {
                    try {
                        inverseListener.onChange(e);
                    } catch (RuntimeException | Error ex) {
                        if (throwable == null) {
                            throwable = ex;
                        }
                    }
                }
            }
            if (newFk != null) {
                AssociationEvent e = new AssociationEvent(inverseProp, newFk, null, childId, con, reason);
                for (AssociationListener inverseListener : inverseListeners) {
                    try {
                        inverseListener.onChange(e);
                    } catch (RuntimeException | Error ex) {
                        if (throwable == null) {
                            throwable = ex;
                        }
                    }
                }
            }
        }
        return throwable;
    }

    @Override
    public void fireMiddleTableDelete(ImmutableProp prop, Object sourceId, Object targetId, Connection con, Object reason) {
        ImmutableProp mappedBy = prop.getMappedBy();
        if (mappedBy != null) {
            fireMiddleTableDeleteImpl(mappedBy, targetId, sourceId, con, reason);
        } else {
            fireMiddleTableDeleteImpl(prop, sourceId, targetId, con, reason);
        }
    }

    private void fireMiddleTableDeleteImpl(ImmutableProp prop, Object sourceId, Object targetId, Connection con, Object reason) {
        ImmutableProp inverseProp = prop.getOpposite();
        List<AssociationListener> listeners = associationListeners(prop);
        List<AssociationListener> inverseListeners = associationListeners(inverseProp);
        Throwable throwable = null;
        if (!listeners.isEmpty()) {
            AssociationEvent e = new AssociationEvent(prop, sourceId, targetId, null, con, reason);
            for (AssociationListener listener : listeners) {
                try {
                    listener.onChange(e);
                } catch (RuntimeException | Error ex) {
                    if (throwable == null) {
                        throwable = ex;
                    }
                }
            }
        }
        if (!inverseListeners.isEmpty()) {
            AssociationEvent e = new AssociationEvent(inverseProp, targetId, sourceId, null, con, reason);
            for (AssociationListener inverseListener : inverseListeners) {
                try {
                    inverseListener.onChange(e);
                } catch (RuntimeException | Error ex) {
                    if (throwable == null) {
                        throwable = ex;
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

    @Override
    public void fireMiddleTableInsert(ImmutableProp prop, Object sourceId, Object targetId, Connection con, Object reason) {
        ImmutableProp mappedBy = prop.getMappedBy();
        if (mappedBy != null) {
            fireMiddleTableInsertImpl(mappedBy, targetId, sourceId, con, reason);
        } else {
            fireMiddleTableInsertImpl(prop, sourceId, targetId, con, reason);
        }
    }

    private void fireMiddleTableInsertImpl(ImmutableProp prop, Object sourceId, Object targetId, Connection con, Object reason) {
        ImmutableProp inverseProp = prop.getOpposite();
        List<AssociationListener> listeners = associationListeners(prop);
        List<AssociationListener> inverseListeners = associationListeners(inverseProp);
        Throwable throwable = null;
        if (!listeners.isEmpty()) {
            AssociationEvent e = new AssociationEvent(prop, sourceId, null, targetId, con, reason);
            for (AssociationListener listener : listeners) {
                try {
                    listener.onChange(e);
                } catch (RuntimeException | Error ex) {
                    if (throwable == null) {
                        throwable = ex;
                    }
                }
            }
        }
        if (!inverseListeners.isEmpty()) {
            AssociationEvent e = new AssociationEvent(inverseProp, targetId, null, sourceId, con, reason);
            for (AssociationListener inverseListener : inverseListeners) {
                try {
                    inverseListener.onChange(e);
                } catch (RuntimeException | Error ex) {
                    if (throwable == null) {
                        throwable = ex;
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

    @Override
    public void fireAssociationEvict(ImmutableProp prop, Object sourceId, Object reason) {
        List<AssociationListener> listeners = associationListeners(prop);
        Throwable throwable = null;
        if (!listeners.isEmpty()) {
            AssociationEvent e = new AssociationEvent(prop, sourceId, null, reason);
            for (AssociationListener listener : listeners) {
                try {
                    listener.onChange(e);
                } catch (RuntimeException | Error ex) {
                    if (throwable == null) {
                        throwable = ex;
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

    private List<EntityListener<ImmutableSpi>> entityListeners(ImmutableType type) {
        List<EntityListener<ImmutableSpi>> listeners = new ArrayList<>(globalEntityListeners);
        Map<ImmutableType, CopyOnWriteArrayList<EntityListener<ImmutableSpi>>> map =
                entityTableListenerMultiMap;
        while (type != null) {
            CopyOnWriteArrayList<EntityListener<ImmutableSpi>> list = map.get(type);
            if (list != null) {
                listeners.addAll(list);
            }
            type = type.getSuperType();
        }
        return listeners;
    }

    private List<AssociationListener> associationListeners(ImmutableProp prop) {
        List<AssociationListener> listeners = new ArrayList<>(globalAssociationListeners);
        CopyOnWriteArrayList<AssociationListener> list = associationListenerMultiMap.get(prop);
        if (list != null) {
            listeners.addAll(list);
        }
        return listeners;
    }
}
