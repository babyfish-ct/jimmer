package org.babyfish.jimmer.sql.event;

import org.babyfish.jimmer.meta.EmbeddedLevel;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.TargetLevel;
import org.babyfish.jimmer.impl.util.Classes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.Objects;

public class AssociationEvent {

    private final ImmutableProp prop;

    private final Object sourceId;

    private final Object detachedTargetId;

    private final Object attachedTargetId;

    private final Connection con;

    private final Object reason;

    public AssociationEvent(
            ImmutableProp prop,
            Object sourceId,
            Connection con, Object reason
    ) {
        validateConstructorArgs(prop, sourceId);
        this.prop = prop;
        this.sourceId = sourceId;
        this.detachedTargetId = null;
        this.attachedTargetId = null;
        this.con = con;
        this.reason = reason;
    }

    public AssociationEvent(
            ImmutableProp prop,
            Object sourceId,
            Object detachedTargetId,
            Object attachedTargetId,
            Connection con, Object reason
    ) {
        validateConstructorArgs(prop, sourceId);
        if (detachedTargetId == null && attachedTargetId == null) {
            throw new IllegalArgumentException(
                    "Both `detachedTargetId` and `attachedTargetId` is null, this is not allowed"
            );
        }
        ImmutableProp targetIdProp = prop.getTargetType().getIdProp();

        boolean validDetachedTargetId;
        if (detachedTargetId == null) {
            validDetachedTargetId = true;
        } else if (targetIdProp.isEmbedded(EmbeddedLevel.SCALAR)) {
            validDetachedTargetId = targetIdProp.getTargetType().getJavaClass().isAssignableFrom(detachedTargetId.getClass());
        } else {
            validDetachedTargetId = Classes.matches(targetIdProp.getElementClass(), detachedTargetId.getClass());
        }
        if (!validDetachedTargetId) {
            throw new IllegalArgumentException(
                    "The type of detachedTargetId \"" +
                            sourceId +
                            "\" does not match the type of \"" +
                            prop.getTargetType().getIdProp() +
                            "\""
            );
        }

        boolean validAttachedTargetId;
        if (attachedTargetId == null) {
            validAttachedTargetId = true;
        } else if (targetIdProp.isEmbedded(EmbeddedLevel.SCALAR)) {
            validAttachedTargetId = targetIdProp.getTargetType().getJavaClass().isAssignableFrom(attachedTargetId.getClass());
        } else {
            validAttachedTargetId = Classes.matches(targetIdProp.getElementClass(), attachedTargetId.getClass());
        }
        if (!validAttachedTargetId) {
            throw new IllegalArgumentException(
                    "The type of attachedTargetId \"" +
                            sourceId +
                            "\" does not match the type of \"" +
                            prop.getTargetType().getIdProp() +
                            "\""
            );
        }

        this.prop = prop;
        this.sourceId = sourceId;
        this.detachedTargetId = detachedTargetId;
        this.attachedTargetId = attachedTargetId;
        this.con = con;
        this.reason = reason;
    }

    private void validateConstructorArgs(ImmutableProp prop, Object sourceId) {
        if (prop == null) {
            throw new IllegalArgumentException("prop cannot be null");
        }
        if (!prop.isAssociation(TargetLevel.ENTITY)) {
            throw new IllegalArgumentException("prop must be association");
        }
        ImmutableProp idProp = prop.getDeclaringType().getIdProp();
        boolean validId = false;
        if (sourceId != null) {
            if (idProp.isEmbedded(EmbeddedLevel.SCALAR)) {
                validId = idProp.getTargetType().getJavaClass().isAssignableFrom(sourceId.getClass());
            } else if (Classes.matches(idProp.getElementClass(), sourceId.getClass())) {
                validId = true;
            }
        }
        if (!validId) {
            throw new IllegalArgumentException(
                    "The type of sourceId \"" +
                            sourceId +
                            "\" does not match the type of \"" +
                            idProp +
                            "\""
            );
        }
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
        validateTarget();
        return detachedTargetId;
    }

    @Nullable
    public Object getAttachedTargetId() {
        validateTarget();
        return attachedTargetId;
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
        return this.con;
    }

    @Nullable
    public Object getReason() {
        return reason;
    }

    @NotNull
    public Type getType() {
        if (detachedTargetId == null && attachedTargetId == null) {
            return Type.EVICT;
        }
        if (detachedTargetId == null) {
            return Type.ATTACH;
        }
        if (attachedTargetId == null) {
            return Type.DETACH;
        }
        return Type.REPLACE;
    }

    private void validateTarget() {
        if (detachedTargetId == null && attachedTargetId == null) {
            throw new IllegalStateException("Cannot get target id because the event type is `EVICT`");
        }
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

    public enum Type {
        DETACH,
        ATTACH,
        REPLACE,
        EVICT
    }
}
