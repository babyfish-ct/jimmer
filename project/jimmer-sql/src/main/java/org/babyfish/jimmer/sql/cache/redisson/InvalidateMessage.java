package org.babyfish.jimmer.sql.cache.redisson;

import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.json.JsonCodec;
import org.babyfish.jimmer.json.JsonTypeFactory;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.beans.ConstructorProperties;
import java.io.Serializable;
import java.util.Collection;
import java.util.UUID;

class InvalidateMessage implements Serializable {

    private static final long serialVersionUID = -2287205502201925856L;

    @NotNull
    UUID trackerId; // No final for serialization

    @NotNull
    String typeName; // No final for serialization

    @Nullable
    String propName; // No final for serialization

    @NotNull
    Collection<?> ids; // No final for serialization

    private transient ImmutableType type;

    InvalidateMessage(
            @NotNull UUID trackerId,
            @NotNull CacheTracker.InvalidateEvent event
    ) {
        this.trackerId = trackerId;
        this.type = event.getType();
        this.typeName = type.toString();
        if (event.getProp() != null) {
            this.propName = event.getProp().getName();
        } else {
            this.propName = null;
        }
        this.ids = event.getIds();
    }

    public InvalidateMessage(
            @NotNull UUID trackerId,
            @NotNull String typeName,
            @Nullable String propName
    ) {
        this.trackerId = trackerId;
        this.typeName = typeName;
        this.propName = propName;
    }

    @ConstructorProperties({"trackerId", "typeName", "propName", "ids"})
    public InvalidateMessage(UUID trackerId, String typeName, String propName, Collection<?> ids) {
        this(trackerId, typeName, propName);
        Class<?> idClass = Classes.boxTypeOf(getType().getIdProp().getElementClass());
        if (ids.stream().allMatch(idClass::isInstance)) {
            this.ids = ids;
        } else {
            try {
                this.ids = JsonCodec.jsonCodec().converter().convert(ids, JsonTypeFactory.INSTANCE.constructListType(idClass));
            } catch (Exception ex) {
                throw new IllegalArgumentException("Cannot read cache IDs for " + typeName, ex);
            }
        }
    }

    public UUID getTrackerId() {
        return trackerId;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getPropName() {
        return propName;
    }

    public Collection<?> getIds() {
        return ids;
    }

    ImmutableType getType() {
        ImmutableType type = this.type;
        if (type == null) {
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
            this.type = type = ImmutableType.get(javaType);
        }
        return type;
    }

    CacheTracker.InvalidateEvent toEvent() {
        if (propName != null) {
            ImmutableProp prop = getType().getProp(propName);
            return new CacheTracker.InvalidateEvent(prop, ids);
        }
        return new CacheTracker.InvalidateEvent(getType(), ids);
    }
}
