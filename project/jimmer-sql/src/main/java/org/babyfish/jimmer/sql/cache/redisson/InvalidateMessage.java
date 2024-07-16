package org.babyfish.jimmer.sql.cache.redisson;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.jackson.ImmutableModule;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        setterVisibility = JsonAutoDetect.Visibility.NONE
)
class InvalidateMessage {

    static final ObjectMapper MAPPER =
            new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .registerModule(new ImmutableModule());

    @NotNull
    final UUID trackerId;

    @NotNull
    final String typeName;

    @Nullable
    final String propName;

    @NotNull
    final String ids;

    @JsonCreator
    InvalidateMessage(
            @JsonProperty("trackerId") @NotNull UUID trackerId,
            @JsonProperty("typeName") @NotNull String typeName,
            @JsonProperty("propName") @Nullable String propName,
            @JsonProperty("ids") @NotNull String ids
    ) {
        this.trackerId = trackerId;
        this.typeName = typeName;
        this.propName = propName;
        this.ids = ids;
    }

    CacheTracker.InvalidationEvent toEvent() {
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
        Collection<?> ids;
        try {
            ids = MAPPER.readValue(
                    this.ids,
                    CollectionType.construct(
                            List.class,
                            null,
                            null,
                            null,
                            SimpleType.constructUnsafe(
                                    Classes.boxTypeOf(type.getIdProp().getReturnClass())
                            )
                    )
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(
                    "Can not parse \"" +
                            "id\" to the type \"" +
                            type.getIdProp().getReturnClass().getName() +
                            "\""
            );
        }
        if (propName != null) {
            ImmutableProp prop = type.getProp(propName);
            return new CacheTracker.InvalidationEvent(prop, ids);
        }
        return new CacheTracker.InvalidationEvent(type, ids);
    }
}
