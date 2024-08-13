package org.babyfish.jimmer.sql.cache.redisson;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.sql.cache.CacheTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@JsonSerialize(using = InvalidateMessage.Serializer.class)
@JsonDeserialize(using = InvalidateMessage.Deserializer.class)
class InvalidateMessage implements Serializable {

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
        this.typeName = event.getType().toString();
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

    static class Serializer extends StdSerializer<InvalidateMessage> {

        Serializer() {
            super(InvalidateMessage.class);
        }

        @Override
        public void serialize(
                InvalidateMessage value,
                JsonGenerator gen,
                SerializerProvider provider
        ) throws IOException {
            gen.writeStartObject();
            gen.writeStringField("trackerId", value.trackerId.toString());
            gen.writeStringField("typeName", value.typeName);
            gen.writeStringField("propName", value.propName);
            gen.writeObjectField("ids", value.ids);
            gen.writeEndObject();
        }
    }

    static class Deserializer extends StdDeserializer<InvalidateMessage> {

        protected Deserializer() {
            super(InvalidateMessage.class);
        }

        @Override
        public InvalidateMessage deserialize(
                JsonParser jp,
                DeserializationContext ctx
        ) throws IOException, JacksonException {
            JsonNode node = jp.getCodec().readTree(jp);
            UUID trackerId = UUID.fromString(node.get("trackerId").asText());
            String typeName = node.get("typeName").asText();
            String propName = node.get("propName").isNull() ? null : node.get("propName").asText();
            InvalidateMessage message = new InvalidateMessage(trackerId, typeName, propName);
            ImmutableProp idProp = message.getType().getIdProp();
            message.ids = ctx.readTreeAsValue(
                    node.get("ids"),
                    CollectionType.construct(
                            List.class,
                            null,
                            null,
                            null,
                            SimpleType.constructUnsafe(idProp.getReturnClass())
                    )
            );
            return message;
        }
    }
}
