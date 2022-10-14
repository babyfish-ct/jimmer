package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.runtime.ImmutableSpi;

import java.util.Objects;

class ForeignKeyAssociationListenerProxy implements EntityListener<ImmutableSpi> {

    private final ImmutableProp prop;

    private final AssociationListener listener;

    ForeignKeyAssociationListenerProxy(ImmutableProp prop, AssociationListener listener) {
        this.prop = prop;
        this.listener = listener;
    }

    @Override
    public void onChange(EntityEvent<ImmutableSpi> e) {
        ImmutableProp fkProp = Utils.primaryAssociationProp(prop);
        Object detachedTargetId = null;
        Object attachedTargetId = null;
        int fkPropId = fkProp.getId();
        int parentIdPropId = fkProp.getTargetType().getIdProp().getId();
        ImmutableSpi oldEntity = e.getOldEntity();
        ImmutableSpi newEntity = e.getNewEntity();
        if (oldEntity != null) {
            if (!oldEntity.__isLoaded(fkPropId)) {
                return;
            }
            ImmutableSpi detachedTarget = (ImmutableSpi) oldEntity.__get(fkPropId);
            if (detachedTarget != null) {
                detachedTargetId = detachedTarget.__get(parentIdPropId);
            }
        }
        if (newEntity != null) {
            if (!newEntity.__isLoaded(fkPropId)) {
                return;
            }
            ImmutableSpi attachedTarget = (ImmutableSpi) newEntity.__get(fkPropId);
            if (attachedTarget != null) {
                attachedTargetId = attachedTarget.__get(parentIdPropId);
            }
        }
        if (Objects.equals(detachedTargetId, attachedTargetId)) {
            return;
        }
        if (fkProp == prop) {
            listener.onChange(new AssociationEvent(prop, e.getId(), detachedTargetId, attachedTargetId, e.getReason()));
        } else {
            if (detachedTargetId != null) {
                listener.onChange(new AssociationEvent(prop, detachedTargetId, e.getId(), null, e.getReason()));
            }
            if (attachedTargetId != null) {
                listener.onChange(new AssociationEvent(prop, attachedTargetId, null, e.getId(), e.getReason()));
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(prop, listener);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForeignKeyAssociationListenerProxy that = (ForeignKeyAssociationListenerProxy) o;
        return prop.equals(that.prop) && listener.equals(that.listener);
    }

    @Override
    public String toString() {
        return "ForeignKeyAssociationProxy{" +
                "prop=" + prop +
                ", listener=" + listener +
                '}';
    }
}
