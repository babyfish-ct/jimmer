package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.meta.ImmutableProp;

import java.util.Objects;

public class AssociationEvent {

    private ImmutableProp prop;

    private Object sourceId;

    private Object detachedTargetId;

    private Object attachedTargetId;

    public AssociationEvent(
            ImmutableProp prop,
            Object sourceId,
            Object detachedTargetId,
            Object attachedTargetId
    ) {
        if (prop == null) {
            throw new IllegalArgumentException("prop cannot be null");
        }
        if (!prop.isAssociation()) {
            throw new IllegalArgumentException("prop must be association");
        }
        if (sourceId == null || sourceId.getClass() != prop.getDeclaringType().getIdProp().getElementClass()) {
            throw new IllegalArgumentException(
                    "The type of sourceId \"" +
                            sourceId +
                            "\" does not match \"" +
                            prop.getDeclaringType().getIdProp() +
                            "\""
            );
        }
        Class<?> targetIdClass = prop.getTargetType().getIdProp().getElementClass();
        if (detachedTargetId != null && detachedTargetId.getClass() != targetIdClass) {
            throw new IllegalArgumentException(
                    "The type of detachedTargetId \"" +
                            sourceId +
                            "\" does not match \"" +
                            prop.getTargetType().getIdProp() +
                            "\""
            );
        }
        if (attachedTargetId != null && attachedTargetId.getClass() != targetIdClass) {
            throw new IllegalArgumentException(
                    "The type of attachedTargetId \"" +
                            sourceId +
                            "\" does not match \"" +
                            prop.getTargetType().getIdProp() +
                            "\""
            );
        }
        this.prop = prop;
        this.sourceId = sourceId;
        this.detachedTargetId = detachedTargetId;
        this.attachedTargetId = attachedTargetId;
    }

    public ImmutableProp getImmutableProp() {
        return prop;
    }

    public Object getSourceId() {
        return sourceId;
    }

    public Object getDetachedTargetId() {
        return detachedTargetId;
    }

    public Object getAttachedTargetId() {
        return attachedTargetId;
    }

    public EventType getEventType() {
        if (detachedTargetId == null) {
            return EventType.INSERT;
        }
        if (attachedTargetId == null) {
            return EventType.DELETE;
        }
        return EventType.UPDATE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(prop, sourceId, detachedTargetId, attachedTargetId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssociationEvent that = (AssociationEvent) o;
        return prop.equals(that.prop) && sourceId.equals(that.sourceId) && Objects.equals(detachedTargetId, that.detachedTargetId) && Objects.equals(attachedTargetId, that.attachedTargetId);
    }

    @Override
    public String toString() {
        return "AssociationEvent{" +
                "prop=" + prop +
                ", sourceId=" + sourceId +
                ", detachedTargetId=" + detachedTargetId +
                ", attachedTargetId=" + attachedTargetId +
                '}';
    }
}
