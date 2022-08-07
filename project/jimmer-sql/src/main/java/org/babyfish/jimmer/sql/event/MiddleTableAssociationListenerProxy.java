package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.meta.ImmutableProp;

import java.util.Objects;

class MiddleTableAssociationListenerProxy implements MiddleTableListener {

    private final ImmutableProp prop;

    private final AssociationListener listener;

    MiddleTableAssociationListenerProxy(ImmutableProp prop, AssociationListener listener) {
        this.prop = prop;
        this.listener = listener;
    }

    @Override
    public void delete(Object sourceId, Object targetId, Object reason) {
        if (prop.getMappedBy() != null) {
            listener.onChange(new AssociationEvent(prop, targetId, sourceId, null, reason));
        } else {
            listener.onChange(new AssociationEvent(prop, sourceId, targetId, null, reason));
        }
    }

    @Override
    public void insert(Object sourceId, Object targetId, Object reason) {
        if (prop.getMappedBy() != null) {
            listener.onChange(new AssociationEvent(prop, targetId, null, sourceId, reason));
        } else {
            listener.onChange(new AssociationEvent(prop, sourceId, null, targetId, reason));
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
        MiddleTableAssociationListenerProxy that = (MiddleTableAssociationListenerProxy) o;
        return prop.equals(that.prop) && listener.equals(that.listener);
    }

    @Override
    public String toString() {
        return "MiddleTableAssociationProxy{" +
                "prop=" + prop +
                ", listener=" + listener +
                '}';
    }
}
