package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import java.util.Objects;

public class EntityEvent<E> {

    private E oldEntity;

    private E newEntity;

    public EntityEvent(E oldEntity, E newEntity) {
        if (oldEntity == null && newEntity == null) {
            throw new IllegalArgumentException("Both oldEntity and newEntity are null");
        }
        if (oldEntity != null && !(oldEntity instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("oldEntity is not immutable object");
        }
        if (newEntity != null && !(newEntity instanceof ImmutableSpi)) {
            throw new IllegalArgumentException("newEntity is not immutable object");
        }
        if (oldEntity != null && newEntity != null) {
            ImmutableSpi oe = (ImmutableSpi) oldEntity;
            ImmutableSpi ne = (ImmutableSpi) newEntity;
            if (oe.__type() != ne.__type()) {
                throw new IllegalArgumentException("oldEntity and newEntity must belong to same type");
            }
            int idPropId = oe.__type().getIdProp().getId();
            if (!oe.__get(idPropId).equals(ne.__get(idPropId))) {
                throw new IllegalArgumentException("oldEntity and newEntity must have same id");
            }
        }
        this.oldEntity = oldEntity;
        this.newEntity = newEntity;
    }

    public E getOldEntity() {
        return oldEntity;
    }

    public E getNewEntity() {
        return newEntity;
    }

    public Object getId() {
        E oe = this.oldEntity;
        int idPropId = getImmutableType().getIdProp().getId();
        if (oe != null) {
            return ((ImmutableSpi) oe).__get(idPropId);
        }
        return ((ImmutableSpi) newEntity).__get(idPropId);
    }

    public ImmutableType getImmutableType() {
        E oe = this.oldEntity;
        if (oe != null) {
            return ((ImmutableSpi) oe).__type();
        }
        return ((ImmutableSpi) newEntity).__type();
    }

    public EventType getEventType() {
        if (oldEntity == null) {
            return EventType.INSERT;
        }
        if (newEntity == null) {
            return EventType.DELETE;
        }
        return EventType.UPDATE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldEntity, newEntity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityEvent<?> event = (EntityEvent<?>) o;
        return Objects.equals(oldEntity, event.oldEntity) && Objects.equals(newEntity, event.newEntity);
    }

    @Override
    public String toString() {
        return "Event{" +
                "oldEntity=" + oldEntity +
                ", newEntity=" + newEntity +
                '}';
    }
}
