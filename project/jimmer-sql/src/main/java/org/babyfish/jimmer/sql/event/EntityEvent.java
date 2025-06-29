package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.Draft;
import org.babyfish.jimmer.ImmutableObjects;
import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.Objects;

public class EntityEvent<E> implements DatabaseEvent {

    private final Object id;

    private final ImmutableType immutableType;

    private final E oldEntity;

    private final E newEntity;

    private final boolean oldLogicalDeleted;

    private final boolean newLogicalDeleted;

    private final Connection con;

    private final Object reason;

    private Type type;

    private EntityEvent(ImmutableType immutableType, Object id, Connection con, Object reason) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.immutableType = Objects.requireNonNull(immutableType, "immutable type cannot be null");
        this.oldEntity = null;
        this.newEntity = null;
        this.oldLogicalDeleted = false;
        this.newLogicalDeleted = false;
        this.con = con;
        this.reason = reason;
    }

    /**
     * Create an evict event which does not support
     * <ul>
     *     <li>{@link #getOldEntity()}</li>
     *     <li>{@link #getNewEntity()}</li>
     *     <li>{@link #getChangedRef(TypedProp.Single)}</li>
     *     <li>{@link #getChangedRef(ImmutableProp)}</li>
     *     <li>{@link #getUnchangedRef(TypedProp.Single)}</li>
     *     <li>{@link #getUnchangedRef(ImmutableProp)}</li>
     *     <li>{@link #getUnchangedValue(TypedProp.Single)}</li>
     *     <li>{@link #getUnchangedRef(ImmutableProp)}</li>
     * </ul>
     * @param immutableType
     * @param id
     * @param con
     * @param reason
     * @return
     * @param <E>
     */
    public static <E> EntityEvent<E> evict(ImmutableType immutableType, Object id, Connection con, Object reason) {
        return new EntityEvent<>(immutableType, id, con, reason);
    }

    public EntityEvent(E oldEntity, E newEntity, Connection con, Object reason) {
        if (oldEntity == null && newEntity == null) {
            throw new IllegalArgumentException("Both `oldEntity` and `newEntity` are null");
        }
        if (oldEntity != null) {
            if (!(oldEntity instanceof ImmutableSpi)) {
                throw new IllegalArgumentException("oldEntity is not immutable object");
            }
            if (oldEntity instanceof Draft) {
                throw new IllegalArgumentException("oldEntity cannot be draft");
            }
        }
        if (newEntity != null) {
            if (!(newEntity instanceof ImmutableSpi)) {
                throw new IllegalArgumentException("newEntity is not immutable object");
            }
            if (newEntity instanceof Draft) {
                throw new IllegalArgumentException("newEntity cannot be draft");
            }
        }
        ImmutableSpi oe = (ImmutableSpi) oldEntity;
        ImmutableSpi ne = (ImmutableSpi) newEntity;
        if (oe != null && ne != null) {
            if (oe.__type() != ne.__type()) {
                throw new IllegalArgumentException("oldEntity and newEntity must belong to same type");
            }
        }
        oldLogicalDeleted = ImmutableObjects.isLogicalDeleted(oldEntity);
        newLogicalDeleted = ImmutableObjects.isLogicalDeleted(newEntity);
        if (oe != null) {
            immutableType = oe.__type();
        } else {
            immutableType = ne.__type();
        }
        PropId idPropId = (oe != null ? oe : ne).__type().getIdProp().getId();
        Object oldId = null;
        if (oe != null && oe.__isLoaded(idPropId)) {
            oldId = oe.__get(idPropId);
        }
        Object newId = null;
        if (ne != null && ne.__isLoaded(idPropId)) {
            newId = ne.__get(idPropId);
        }
        if (oldId == null && newId == null) {
            throw new IllegalStateException("Id is not specified");
        }
        if (oldId != null && newId != null) {
            if (!oldId.equals(newId)) {
                throw new IllegalStateException("Conflict ids in old entity and new entity");
            }
        }
        this.id = oldId != null ? oldId : newId;
        this.oldEntity = oldEntity;
        this.newEntity = newEntity;
        this.con = con;
        this.reason = reason;
    }

    /**
     * Get the old entity no matter it is logically deleted or not
     *
     * <p>This method is not supported by evict event by throwing IllegalStateException</p>
     *
     * @return The old entity
     * @exception IllegalStateException The current event is an evict event
     */
    @Nullable
    public E getOldEntity() {
        validateState();
        return oldLogicalDeleted ? null : oldEntity;
    }

    /**
     * Get the new entity no matter it is logically deleted or not
     *
     * <p>This method is not supported by evict event by throwing IllegalStateException</p>
     *
     * @return The new entity
     * @exception IllegalStateException The current event is an evict event
     */
    @Nullable
    public E getNewEntity() {
        validateState();
        return newLogicalDeleted ? null : newEntity;
    }

    @NotNull
    public Object getId() {
        return this.id;
    }

    @Override
    public boolean isChanged(ImmutableProp prop) {
        if (!prop.getDeclaringType().isAssignableFrom(getImmutableType())) {
            return false;
        }
        return getChangedRef(prop) != null;
    }

    @Override
    public boolean isChanged(TypedProp<?, ?> prop) {
        return isChanged(prop.unwrap());
    }

    @Override
    @Nullable
    public Connection getConnection() {
        return con;
    }

    @Override
    @Nullable
    public Object getReason() {
        return this.reason;
    }

    @NotNull
    public ImmutableType getImmutableType() {
        return immutableType;
    }

    @NotNull
    public Type getType() {
        Type type = this.type;
        if (type == null) {
            this.type = type = getType0();
        }
        return type;
    }

    private Type getType0() {
        E oe = oldEntity;
        E ne = newEntity;
        if (oe == null && ne == null) {
            return Type.EVICT;
        }
        if (oe == null) {
            return Type.INSERT;
        }
        if (ne == null) {
            return Type.DELETE;
        }
        if (oldLogicalDeleted && !newLogicalDeleted) {
            return Type.LOGICAL_INSERTED;
        }
        if (!oldLogicalDeleted && newLogicalDeleted) {
            return Type.LOGICAL_DELETED;
        }
        return Type.UPDATE;
    }

    /**
     * Is the current event an evict event which does not support
     * <ul>
     *     <li>{@link #getOldEntity()}</li>
     *     <li>{@link #getNewEntity()}</li>
     *     <li>{@link #getChangedRef(TypedProp.Single)}</li>
     *     <li>{@link #getChangedRef(ImmutableProp)}</li>
     *     <li>{@link #getUnchangedRef(TypedProp.Single)}</li>
     *     <li>{@link #getUnchangedRef(ImmutableProp)}</li>
     *     <li>{@link #getUnchangedValue(TypedProp.Single)}</li>
     *     <li>{@link #getUnchangedRef(ImmutableProp)}</li>
     * </ul>
     * @return Is the current event an evict event
     */
    @Override
    public boolean isEvict() {
        return oldEntity == null && newEntity == null;
    }

    private void validateState() {
        if (oldEntity == null && newEntity == null) {
            throw new IllegalStateException("Cannot get information except id and immutable type because the event type is `EVICT`");
        }
    }

    /**
     * Get the unchanged ref of specified property
     *
     * <p>This method is not supported by evict event by throwing IllegalStateException</p>
     *
     * <p>If old/new entity is not null but is logically deleted, old/new entity will be considered as null</p>
     *
     * @param prop The specified property
     * @param <T> The return type of specified property
     * @return If the value of specified property is NOT changed,
     * return a ref object which is the wrapper of unchanged value;
     * otherwise, return null.
     * @exception IllegalStateException The current event type is EVICT
     * @exception IllegalArgumentException The declaring type of
     * specified property is not assignable from the entity type
     * of current event object
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> Ref<T> getUnchangedRef(ImmutableProp prop) {
        validateProp(prop);
        if (!prop.isColumnDefinition()) {
            throw new IllegalArgumentException(
                    "Cannot get the unchanged the value of \"" +
                            prop +
                            "\" " +
                            "because it is not a property mapped by database columns"
            );
        }
        PropId propId = prop.getId();
        ImmutableSpi oe = (ImmutableSpi) getOldEntity();
        ImmutableSpi ne = (ImmutableSpi) getNewEntity();
        if (ImmutableObjects.isLogicalDeleted(ne)) {
            ne = null;
        }
        boolean oldLoaded = oe != null && oe.__isLoaded(propId);
        boolean newLoaded = ne != null && ne.__isLoaded(propId);
        if (!oldLoaded && !newLoaded) {
            throw new IllegalStateException(
                    "Cannot get the unchanged the value of \"" +
                            prop +
                            "\" " +
                            "from neither oldEntity nor newEntity"
            );
        }
        if (!oldLoaded) {
            return Ref.of((T)ne.__get(propId));
        }
        if (!newLoaded) {
            return Ref.of((T)oe.__get(propId));
        }
        T oldValue = (T)oe.__get(propId);
        T newValue = (T)ne.__get(propId);
        if (valueEqual(prop, oldValue, newValue)) {
            return Ref.of(oldValue);
        }
        return null;
    }

    /**
     * Get the unchanged ref of specified property
     *
     * <p>This method is not supported by evict event by throwing IllegalStateException</p>
     *
     * <p>If old/new entity is not null but is logically deleted, old/new entity will be considered as null</p>
     *
     * @param prop The specified property
     * @param <T> The return type of specified property
     * @return If the value of specified property is NOT changed,
     * return a ref object which is the wrapper of unchanged value;
     * otherwise, return null.
     * @exception IllegalStateException The current event type is EVICT
     * @exception IllegalArgumentException The declaring type of
     * specified property is not assignable from the entity type
     * of current event object
     */
    @Nullable
    public <T> Ref<T> getUnchangedRef(TypedProp.Single<?, T> prop) {
        return getUnchangedRef(prop.unwrap());
    }

    /**
     * Get the changed ref of specified property
     *
     * <p>This method is not supported by evict event by throwing IllegalStateException</p>
     *
     * <p>If old/new entity is not null but is logically deleted, old/new entity will be considered as null</p>
     *
     * @param prop The specified property
     * @param <T> The return type of specified property
     * @return If the value of specified property is NOT changed,
     * return a changed ref object which is a wrapper of
     * both old value and new value;
     * otherwise, return null.
     * @exception IllegalStateException The current event type is EVICT
     * @exception IllegalArgumentException The declaring type of
     * specified property is not assignable from the entity type
     * of current event object
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> ChangedRef<T> getChangedRef(ImmutableProp prop) {
        validateProp(prop);
        if (!prop.isColumnDefinition()) {
            throw new IllegalArgumentException(
                    "Cannot get the unchanged the value of \"" +
                            prop +
                            "\" " +
                            "because it is not a property mapped by database columns"
            );
        }
        PropId propId = prop.getId();
        ImmutableSpi oe = (ImmutableSpi) oldEntity;
        if (ImmutableObjects.isLogicalDeleted(oe)) {
            oe = null;
        }
        ImmutableSpi ne = (ImmutableSpi) newEntity;
        if (ImmutableObjects.isLogicalDeleted(ne)) {
            ne = null;
        }
        if (oe == null && ne == null) {
            return null;
        }
        if (oe == null) {
            if (!ne.__isLoaded(propId)) {
                return null;
            }
            T newValue = (T)ne.__get(propId);
            if (newValue == null) {
                return null;
            }
            return new ChangedRef<>(null, newValue);
        } else if (ne == null) {
            if (!oe.__isLoaded(propId)) {
                return null;
            }
            T oldValue = (T)oe.__get(propId);
            if (oldValue == null) {
                return null;
            }
            return new ChangedRef<>(oldValue, null);
        } else {
            if (!oe.__isLoaded(propId) || !ne.__isLoaded(propId)) {
                return null;
            }
            T oldValue = (T)oe.__get(propId);
            T newValue = (T)ne.__get(propId);
            if (valueEqual(prop, oldValue, newValue)) {
                return null;
            }
            return new ChangedRef<>(oldValue, newValue);
        }
    }

    /**
     * Get the changed ref of specified property
     *
     * <p>This method is not supported by evict event by throwing IllegalStateException</p>
     *
     * <p>If old/new entity is not null but is logically deleted, old/new entity will be considered as null</p>
     *
     * @param prop The specified property
     * @param <T> The return type of specified property
     * @return If the value of specified property is NOT changed,
     * return a changed ref object which is a wrapper of
     * both old value and new value;
     * otherwise, return null.
     * @exception IllegalStateException The current event type is EVICT
     * @exception IllegalArgumentException The declaring type of
     * specified property is not assignable from the entity type
     * of current event object
     */
    @Nullable
    public <T> ChangedRef<T> getChangedRef(TypedProp.Single<?, T> prop) {
        return getChangedRef(prop.unwrap());
    }

    /**
     * Get the value of specified property if it is not changed.
     *
     * <p>This method is not supported by evict event by throwing IllegalStateException</p>
     *
     * <p>If old/new entity is not null but is logically deleted, old/new entity will be considered as null</p>
     *
     * @param prop The specified property
     * @param <T> The return type of returned property
     * @return The unchanged value of specified property
     * @exception IllegalStateException The current event type is EVICT
     * @exception IllegalArgumentException
     * <ul>
     *     <li>The declaring type of
     *     specified property is not assignable from the entity type
     *     of current event object</li>
     *     <li>The value of specified property is changed</li>
     * </ul>
     */
    public <T> T getUnchangedValue(ImmutableProp prop) {
        Ref<T> ref = getUnchangedRef(prop);
        if (ref == null) {
            throw new IllegalArgumentException(
                    "Cannot get unchanged value of \"" +
                            prop +
                            "\", because the value of it is changed"
            );
        }
        return ref.getValue();
    }

    /**
     * Get the value of specified property if it is not changed.
     *
     * <p>This method is not supported by evict event by throwing IllegalStateException</p>
     *
     * <p>If old/new entity is not null but is logically deleted, old/new entity will be considered as null</p>
     *
     * @param prop The specified property
     * @param <T> The return type of returned property
     * @return The unchanged value of specified property
     * @exception IllegalStateException The current event type is EVICT
     * @exception IllegalArgumentException
     * <ul>
     *     <li>The declaring type of
     *     specified property is not assignable from the entity type
     *     of current event object</li>
     *     <li>The value of specified property is changed</li>
     * </ul>
     */
    public <T> T getUnchangedValue(TypedProp.Single<?, T> prop) {
        return getUnchangedValue(prop.unwrap());
    }

    private void validateProp(ImmutableProp prop) {
        validateState();
        if (!prop.getDeclaringType().isAssignableFrom(getImmutableType())) {
            throw new IllegalArgumentException(
                    "The argument `prop` cannot be \"" +
                            prop +
                            "\", it declaring type \"" +
                            prop.getDeclaringType() +
                            "\" is not assignable from the current type \"" +
                            getImmutableType() +
                            "\""
            );
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldEntity, newEntity, reason);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityEvent<?> event = (EntityEvent<?>) o;
        return Objects.equals(oldEntity, event.oldEntity) &&
                Objects.equals(newEntity, event.newEntity) &&
                Objects.equals(reason, event.reason);
    }

    @Override
    public String toString() {
        if (oldEntity == null && newEntity == null) {
            return "EntityEvent{" +
                    "id=" + id +
                    ", immutableType=" + immutableType +
                    ", con=" + con +
                    ", reason=" + reason +
                    '}';
        }
        Type type = getType();
        if (type == Type.LOGICAL_INSERTED || type == Type.LOGICAL_DELETED) {
            return "Event{" +
                    "type=" + type +
                    ", oldEntity=" + oldEntity +
                    ", newEntity=" + newEntity +
                    ", reason=" + reason +
                    '}';
        }
        return "Event{" +
                "oldEntity=" + oldEntity +
                ", newEntity=" + newEntity +
                ", reason=" + reason +
                '}';
    }

    @SuppressWarnings("unchecked")
    private boolean valueEqual(ImmutableProp prop, Object a, Object b) {
        if (!prop.isReference(TargetLevel.PERSISTENT)) {
            return Objects.equals(a, b);
        }
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        PropId targetIdPropId = prop.getTargetType().getIdProp().getId();
        Object targetId1 = ((ImmutableSpi) a).__get(targetIdPropId);
        Object targetId2 = ((ImmutableSpi) b).__get(targetIdPropId);
        return targetId1.equals(targetId2);
    }

    public enum Type {
        EVICT,
        INSERT,
        DELETE,
        LOGICAL_INSERTED,
        LOGICAL_DELETED,
        UPDATE,
    }
}
