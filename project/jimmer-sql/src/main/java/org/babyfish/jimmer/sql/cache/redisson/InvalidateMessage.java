package org.babyfish.jimmer.sql.cache.redisson;

import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;

class InvalidateMessage implements Serializable {

    @NotNull
    UUID trackerId; // No final for serialization

    @NotNull
    String typeName; // No final for serialization

    @Nullable
    String propName; // No final for serialization

    @NotNull
    Collection<?> ids; // No final for serialization

    InvalidateMessage(
            @NotNull UUID trackerId,
            @NotNull CacheTracker.InvalidateEvent event
    ) {
        this.trackerId = trackerId;
        this.typeName = event.getType().toString();
        if (event.getProp() != null) {
            this.propName = event.getProp().getName();
        } else {
            this.propName = null;
        }
        this.ids = event.getIds();
    }

    CacheTracker.InvalidateEvent toEvent() {
        Class<?> javaType;
        try {
            javaType = Class.forName(typeName, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Cannot resolve the type name \"" +
                            typeName +
                            "\""
            );
        }
        ImmutableType type = ImmutableType.get(javaType);
        if (propName != null) {
            ImmutableProp prop = type.getProp(propName);
            return new CacheTracker.InvalidateEvent(prop, ids);
        }
        return new CacheTracker.InvalidateEvent(type, ids);
    }
}
