package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.lang.Ref;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.meta.TypedProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;
import org.babyfish.jimmer.sql.meta.ColumnDefinition;
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
            throw new IllegalArgumentException("Both oldEntity and newEntity are null");
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
        int idPropId = (oe != null ? oe : ne).__type().getIdProp().getId();
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
     * <ul>
     *  <li>If the event is fired by binlog trigger, returns null</li>
     *  <li>If the event is fired by transaction trigger, returns current trigger</li>
     * </ul>
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

    @Nullable
    public <T> Ref<T> getUnchangedFieldRef(ImmutableProp prop) {
        return getUnchangedFieldRef(prop.getId());
    }

    @Nullable
    public <T> Ref<T> getUnchangedFieldRef(TypedProp<?, ?> prop) {
        return getUnchangedFieldRef(prop.unwrap().getId());
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> Ref<T> getUnchangedFieldRef(int propId) {
        ImmutableProp prop = getImmutableType().getProp(propId);
        if (!(prop.getStorage() instanceof ColumnDefinition)) {
            throw new IllegalArgumentException(
                    "Cannot get the unchanged the value of \"" +
                            prop +
                            "\" " +
                            "because it is not a property mapped by database columns"
            );
        }
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

    @Nullable
    public <T> ChangedRef<T> getChangedFieldRef(ImmutableProp prop) {
        return getChangedFieldRef(prop.getId());
    }

    @Nullable
    public <T> ChangedRef<T> getChangedFieldRef(TypedProp<?, ?> prop) {
        return getChangedFieldRef(prop.unwrap().getId());
    }

    @SuppressWarnings("unchecked")
    @Nullable
    public <T> ChangedRef<T> getChangedFieldRef(int propId) {
        ImmutableProp prop = getImmutableType().getProp(propId);
        if (!(prop.getStorage() instanceof ColumnDefinition)) {
            throw new IllegalArgumentException(
                    "Cannot get the unchanged the value of \"" +
                            prop +
                            "\" " +
                            "because it is not a property mapped by database columns"
            );
        }
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
        if (!prop.isReference(TargetLevel.ENTITY)) {
            return Objects.equals(a, b);
        }
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        int targetIdPropId = prop.getTargetType().getIdProp().getId();
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
