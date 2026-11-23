package org.babyfish.jimmer.sql.cache.redisson;

import io.netty.buffer.Unpooled;
import org.babyfish.jimmer.impl.util.Classes;
import org.babyfish.jimmer.json.JsonCodec;
import org.babyfish.jimmer.json.JsonReader;
import org.babyfish.jimmer.json.JsonWriter;
import org.babyfish.jimmer.json.Node;
import org.babyfish.jimmer.lang.Generics;
import org.redisson.client.codec.BaseCodec;
import org.redisson.client.protocol.Decoder;
import org.redisson.client.protocol.Encoder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

final class InvalidateMessageCodec extends BaseCodec {

    private final Encoder encoder;
    private final Decoder<Object> decoder;

    InvalidateMessageCodec(JsonCodec codec) {
        JsonWriter<String> stringWriter = codec.writerFor(String.class);
        JsonReader<Node> reader = codec.treeReader();
        encoder = value -> {
            InvalidateMessage message = (InvalidateMessage) value;
            try {
                Class<?> idClass = Classes.boxTypeOf(message.getType().getIdProp().getElementClass());
                // Older listeners require a literal null member for an entity-wide event.
                String json = "{\"trackerId\":" + stringWriter.writeAsString(message.trackerId.toString()) +
                        ",\"typeName\":" + stringWriter.writeAsString(message.typeName) +
                        ",\"propName\":" + (message.propName == null ? "null" : stringWriter.writeAsString(message.propName)) +
                        ",\"ids\":" + codec.writerFor(tf -> tf.constructCollectionType(Collection.class, idClass))
                                .writeAsString(message.ids) + '}';
                return Unpooled.copiedBuffer(json, StandardCharsets.UTF_8);
            } catch (Exception ex) {
                throw new IOException("Cannot encode cache invalidation", ex);
            }
        };
        decoder = (buf, state) -> {
            try {
                Node node = reader.read(buf.toString(StandardCharsets.UTF_8));
                Node propName = node.get("propName");
                InvalidateMessage message = new InvalidateMessage(
                        UUID.fromString(node.get("trackerId").convertTo(String.class, codec.converter())),
                        node.get("typeName").convertTo(String.class, codec.converter()),
                        propName == null || propName.isNull() ? null : propName.convertTo(String.class, codec.converter())
                );
                Class<?> idClass = Classes.boxTypeOf(message.getType().getIdProp().getElementClass());
                message.ids = node.get("ids").convertTo(Generics.makeParameterizedType(List.class, idClass), codec.converter());
                return message;
            } catch (Exception ex) {
                throw new IOException("Cannot decode cache invalidation", ex);
            }
        };
    }

    @Override
    public Encoder getValueEncoder() {
        return encoder;
    }

    @Override
    public Decoder<Object> getValueDecoder() {
        return decoder;
    }
}
