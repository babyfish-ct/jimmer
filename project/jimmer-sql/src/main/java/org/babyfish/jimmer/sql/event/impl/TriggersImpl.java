package org.babyfish.jimmer.sql.event.impl;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.ast.Expression;
import org.babyfish.jimmer.sql.ast.impl.query.Queries;
import org.babyfish.jimmer.sql.event.*;
import org.babyfish.jimmer.sql.meta.MetadataStrategy;
import org.babyfish.jimmer.sql.runtime.ExecutionPurpose;
import org.babyfish.jimmer.sql.runtime.JSqlClientImplementor;

import java.sql.Connection;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class TriggersImpl implements Triggers {

    private final boolean transaction;

    private JSqlClientImplementor sqlClient;

    private final CopyOnWriteArrayList<EntityListener<ImmutableSpi>> globalEntityListeners =
            new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<AssociationListener> globalAssociationListeners =
            new CopyOnWriteArrayList<>();

    private final ConcurrentMap<ImmutableType, CopyOnWriteArrayList<EntityListener<ImmutableSpi>>> entityTableListenerMultiMap =
            new ConcurrentHashMap<>();

    private final ConcurrentMap<ImmutableProp, CopyOnWriteArrayList<AssociationListener>> associationListenerMultiMap =
            new ConcurrentHashMap<>();

    public TriggersImpl(boolean transaction) {
        this.transaction = transaction;
    }

    public void initialize(JSqlClientImplementor sqlClient) {
        if (this.sqlClient != null) {
            throw new IllegalStateException("sqlClient cannot be changed after initialized");
        }
        if (sqlClient == null) {
            throw new IllegalArgumentException("sqlClient cannot be null");
        }
        this.sqlClient = sqlClient;
    }

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
        throwable = fireAssociationEventByEntityEvent(event, throwable);
        if (throwable instanceof RuntimeException) {
            throw (RuntimeException)throwable;
        }
        if (throwable != null) {
            throw (Error)throwable;
        }
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
    public void fireEntityEvict(ImmutableType type, Object sourceId, Connection con, Object reason) {
        EvictContext ctx = EvictContext.get();
        if (ctx != null && !ctx.add(type, sourceId)) {
            return;
        }
        List<EntityListener<ImmutableSpi>> listeners = entityListeners(type);
        if (!listeners.isEmpty()) {
            Throwable throwable = null;
            EntityEvent<ImmutableSpi> e = EntityEvent.evict(type, sourceId, con, reason);
            for (EntityListener<ImmutableSpi> listener : listeners) {
                try {
                    listener.onChange(e);
                } catch (RuntimeException | Error ex) {
                    if (throwable == null) {
                        throwable = ex;
                    }
                }
            }
            throwable = fireAssociationEventByEntityEvent(e, throwable);
            if (throwable instanceof RuntimeException) {
                throw (RuntimeException)throwable;
            }
            if (throwable != null) {
                throw (Error)throwable;
            }
        }
    }

    @Override
    public void fireAssociationEvict(ImmutableProp prop, Object sourceId, Connection con, Object reason) {
        EvictContext ctx = EvictContext.get();
        if (ctx != null && !ctx.add(prop, sourceId)) {
            return;
        }
        List<AssociationListener> listeners = associationListeners(prop);
        if (!listeners.isEmpty()) {
            Throwable throwable = null;
            AssociationEvent e = new AssociationEvent(prop, sourceId, con, reason);
            for (AssociationListener listener : listeners) {
                try {
                    listener.onChange(e);
                } catch (RuntimeException | Error ex) {
                    if (throwable == null) {
                        throwable = ex;
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

    private List<EntityListener<ImmutableSpi>> entityListeners(ImmutableType type) {
        List<EntityListener<ImmutableSpi>> listeners = new ArrayList<>(globalEntityListeners);
        Map<ImmutableType, CopyOnWriteArrayList<EntityListener<ImmutableSpi>>> map =
                entityTableListenerMultiMap;
        for (ImmutableType t : type.getAllTypes()) {
            CopyOnWriteArrayList<EntityListener<ImmutableSpi>> list = map.get(t);
            if (list != null) {
                listeners.addAll(list);
            }
        }
        return listeners;
    }

    private List<AssociationListener> associationListeners(ImmutableProp prop) {
        if (prop == null) {
            return Collections.emptyList();
        }
        List<AssociationListener> listeners = new ArrayList<>(globalAssociationListeners);
        CopyOnWriteArrayList<AssociationListener> list = associationListenerMultiMap.get(prop);
        if (list != null) {
            listeners.addAll(list);
        }
        return listeners;
    }

    @Override
    public boolean isTransaction() {
        return transaction;
    }

    private Throwable fireAssociationEventByEntityEvent(EntityEvent<?> event, Throwable throwable) {
        ImmutableType type = event.getImmutableType();
        if (!event.isEvict()) {
            for (ImmutableProp prop : type.getProps().values()) {
                if (prop.isColumnDefinition() && prop.isAssociation(TargetLevel.PERSISTENT)) {
                    ChangedRef<Object> changedRef = event.getChangedRef(prop);
                    if (changedRef != null) {
                        ChangedRef<Object> fkRef = changedRef.toIdRef();
                        Object childId = event.getId();
                        Object oldFk = fkRef.getOldValue();
                        Object newFk = fkRef.getNewValue();
                        Connection con = event.getConnection();
                        Object reason = event.getReason();
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
                    }
                }
            }
        }
        MetadataStrategy metadataStrategy = sqlClient.getMetadataStrategy();
        ImmutableType thisType = event.getImmutableType();
        for (ImmutableProp backProp : sqlClient.getEntityManager().getAllBackProps(thisType)) {
            if (!backProp.isAssociation(TargetLevel.PERSISTENT)) {
                continue;
            }
            EvictContext ctx = EvictContext.get();
            if (ctx != null && !ctx.isAllowed(backProp)) {
                continue;
            }
            if (!event.isEvict() && backProp.isTargetForeignKeyReal(metadataStrategy)) {
                continue;
            }
            if (!event.isEvict()) {
                ImmutableProp mappedBy = backProp.getMappedBy();
                if (mappedBy != null && mappedBy.isTargetForeignKeyReal(metadataStrategy)) {
                    continue;
                }
            }
            List<?> backRefIds = BackRefIds.findBackRefIds(sqlClient, backProp, event.getId(), event.getConnection());
            for (Object backRefId : backRefIds) {
                try {
                    fireAssociationEvict(backProp, backRefId, event.getConnection());
                } catch (RuntimeException | Error ex) {
                    if (throwable == null) {
                        throwable = ex;
                    }
                }
            }
        }
        return throwable;
    }
}
