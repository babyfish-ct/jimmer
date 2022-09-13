package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.util.Classes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class AssociationEvent {

    private final ImmutableProp prop;

    private final Object sourceId;

    private final Object detachedTargetId;

    private final Object attachedTargetId;

    private final Object reason;

    public AssociationEvent(
            ImmutableProp prop,
            Object sourceId,
            Object detachedTargetId,
            Object attachedTargetId,
            Object reason
    ) {
        if (prop == null) {
            throw new IllegalArgumentException("prop cannot be null");
        }
        if (!prop.isAssociation(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException("prop must be association");
        }
        if (sourceId == null || !Classes.matches(prop.getDeclaringType().getIdProp().getElementClass(), sourceId.getClass())) {
            throw new IllegalArgumentException(
                    "The type of sourceId \"" +
                            sourceId +
                            "\" does not match \"" +
                            prop.getDeclaringType().getIdProp() +
                            "\""
            );
        }
        Class<?> expectedTargetIdClass = prop.getTargetType().getIdProp().getElementClass();
        if (detachedTargetId != null && !Classes.matches(expectedTargetIdClass, detachedTargetId.getClass())) {
            throw new IllegalArgumentException(
                    "The type of detachedTargetId \"" +
                            sourceId +
                            "\" does not match \"" +
                            prop.getTargetType().getIdProp() +
                            "\""
            );
        }
        if (attachedTargetId != null && !Classes.matches(expectedTargetIdClass, attachedTargetId.getClass())) {
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
        this.reason = reason;
    }

    @NotNull
    public ImmutableProp getImmutableProp() {
        return prop;
    }

    @NotNull
    public Object getSourceId() {
        return sourceId;
    }

    @Nullable
    public Object getDetachedTargetId() {
        return detachedTargetId;
    }

    @Nullable
    public Object getAttachedTargetId() {
        return attachedTargetId;
    }

    @Nullable
    public Object getReason() {
        return reason;
    }

    @NotNull
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
        return Objects.hash(prop, sourceId, detachedTargetId, attachedTargetId, reason);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AssociationEvent that = (AssociationEvent) o;
        return prop.equals(that.prop) &&
                sourceId.equals(that.sourceId) &&
                Objects.equals(detachedTargetId, that.detachedTargetId) &&
                Objects.equals(attachedTargetId, that.attachedTargetId) &&
                Objects.equals(reason, that.reason);
    }

    @Override
    public String toString() {
        return "AssociationEvent{" +
                "prop=" + prop +
                ", sourceId=" + sourceId +
                ", detachedTargetId=" + detachedTargetId +
                ", attachedTargetId=" + attachedTargetId +
                ", reason=" + reason +
                '}';
    }
}
