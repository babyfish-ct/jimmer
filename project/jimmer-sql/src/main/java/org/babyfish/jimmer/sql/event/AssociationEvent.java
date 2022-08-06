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
        if (sourceId == null || !matched(prop.getDeclaringType().getIdProp().getElementClass(), sourceId.getClass())) {
            throw new IllegalArgumentException(
                    "The type of sourceId \"" +
                            sourceId +
                            "\" does not match \"" +
                            prop.getDeclaringType().getIdProp() +
                            "\""
            );
        }
        Class<?> expectedTargetIdClass = prop.getTargetType().getIdProp().getElementClass();
        if (detachedTargetId != null && !matched(expectedTargetIdClass, detachedTargetId.getClass())) {
            throw new IllegalArgumentException(
                    "The type of detachedTargetId \"" +
                            sourceId +
                            "\" does not match \"" +
                            prop.getTargetType().getIdProp() +
                            "\""
            );
        }
        if (attachedTargetId != null && !matched(expectedTargetIdClass, attachedTargetId.getClass())) {
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

    private static boolean matched(Class<?> expectedType, Class<?> actualType) {
        if (expectedType == actualType) {
            return true;
        }
        if (expectedType == boolean.class) {
            return actualType == Boolean.class;
        }
        if (expectedType == char.class) {
            return actualType == Character.class;
        }
        if (expectedType == byte.class) {
            return actualType == Byte.class;
        }
        if (expectedType == short.class) {
            return actualType == Short.class;
        }
        if (expectedType == int.class) {
            return actualType == Integer.class;
        }
        if (expectedType == long.class) {
            return actualType == Long.class;
        }
        if (expectedType == float.class) {
            return actualType == Float.class;
        }
        if (expectedType == double.class) {
            return actualType == Double.class;
        }
        return false;
    }
}
