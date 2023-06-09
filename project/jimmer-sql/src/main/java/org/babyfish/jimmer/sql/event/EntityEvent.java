package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.*;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.Objects;

public class EntityEvent<E> {

    private final Object id;

    private final E oldEntity;

    private final E newEntity;

    private final Connection con;

    private final Object reason;

    public EntityEvent(E oldEntity, E newEntity, Connection con, Object reason) {
        if (oldEntity == null && newEntity == null) {
            throw new IllegalArgumentException("Both `oldEntity` and `newEntity` are null");
        }
        if (oldEntity != null && !(oldEntity instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("oldEntity is not immutable object");
        }
        if (newEntity != null && !(newEntity instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("newEntity is not immutable object");
        }
        ImmutableSpi oe = (ImmutableSpi) oldEntity;
        ImmutableSpi ne = (ImmutableSpi) newEntity;
        if (oe != null && ne != null) {
            if (oe.__type() != ne.__type()) {
                throw new IllegalArgumentException("oldEntity and newEntity must belong to same type");
            }
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

    @Nullable
    public E getOldEntity() {
        return oldEntity;
    }

    @Nullable
    public E getNewEntity() {
        return newEntity;
    }

    @NotNull
    public Object getId() {
        return this.id;
    }

    /**
     * Determine whether the trigger for sending the current event is within
     * a transaction or based on binlog
     *
     * <ul>
     *  <li>If the event is fired by binlog trigger, returns null</li>
     *  <li>If the event is fired by transaction trigger, returns current trigger</li>
     * </ul>
     *
     * <p>
     *     Notes, If you use jimmer in spring-boot and accept events with `@EventListener`,
     *     it will be very important to determine whether this property is null.
     *     Because once the `triggerType` of `SqlClient` is set to `BOTH`, the same event
     *     will be notified twice.
     * </p>
     *
     * @return The current connection or null
     */
    @Nullable
    public Connection getConnection() {
        return con;
    }

    @Nullable
    public Object getReason() {
        return this.reason;
    }

    @NotNull
    public ImmutableType getImmutableType() {
        E oe = this.oldEntity;
        if (oe != null) {
            return ((ImmutableSpi) oe).__type();
        }
        return ((ImmutableSpi) newEntity).__type();
    }

    @NotNull
    public Type getType() {
        if (oldEntity == null) {
            return Type.INSERT;
        }
        if (newEntity == null) {
            return Type.DELETE;
        }
        return Type.UPDATE;
    }

    /**
     * Get the unchanged ref of specified property
     * @param prop The specified property
     * @param <T> The return type of specified property
     * @return If the value of specified property is NOT changed,
     * return a ref object which is the wrapper of unchanged value;
     * otherwise, return null.
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
        ImmutableSpi oe = (ImmutableSpi) oldEntity;
        ImmutableSpi ne = (ImmutableSpi) newEntity;
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
     * @param prop The specified property
     * @param <T> The return type of specified property
     * @return If the value of specified property is NOT changed,
     * return a ref object which is the wrapper of unchanged value;
     * otherwise, return null.
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
     * @param prop The specified property
     * @param <T> The return type of specified property
     * @return If the value of specified property is NOT changed,
     * return a changed ref object which is a wrapper of
     * both old value and new value;
     * otherwise, return null.
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
        ImmutableSpi ne = (ImmutableSpi) newEntity;
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
     * @param prop The specified property
     * @param <T> The return type of specified property
     * @return If the value of specified property is NOT changed,
     * return a changed ref object which is a wrapper of
     * both old value and new value;
     * otherwise, return null.
     * @exception IllegalArgumentException The declaring type of
     * specified property is not assignable from the entity type
     * of current event object
     */
    @Nullable
    public <T> ChangedRef<T> getChangedRef(TypedProp.Single<?, T> prop) {
        return getChangedRef(prop.unwrap());
    }

    /**
     * Is the value of specified property changed?
     * <p>Note: If the declaring type
     * of specified property is not assignable from the entity type
     * of current event object, it returns false</p>
     * @param prop The specified property
     * @return Whether the declaring type
     * of specified property is assignable from the entity type
     * of current event object and the value of specified property is changed.
     */
    public boolean isChanged(ImmutableProp prop) {
        if (!prop.getDeclaringType().isAssignableFrom(getImmutableType())) {
            return false;
        }
        return getChangedRef(prop) != null;
    }

    /**
     * Is the value of specified property changed?
     * <p>Note: If the declaring type
     * of specified property is not assignable from the entity type
     * of current event object, it returns false</p>
     * @param prop The specified property
     * @return Whether the declaring type
     * of specified property is assignable from the entity type
     * of current event object and the value of specified property is changed.
     */
    public boolean isChanged(TypedProp.Single<?, ?> prop) {
        return getChangedRef(prop.unwrap()) != null;
    }

    /**
     * Get the value of specified property if it is not changed.
     * @param prop The specified property
     * @param <T> The return type of returned property
     * @return The unchanged value of specified property
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
     * @param prop The specified property
     * @param <T> The return type of returned property
     * @return The unchanged value of specified property
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
        if (!prop.getDeclaringType().isAssignableFrom(getImmutableType())) {
            throw new IllegalArgumentException(
                    "The argument `prop` cannot be \"prop" +
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
        DELETE,
        INSERT,
        UPDATE,
    }
}
