package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;

import java.io.IOException;

public class ImmutableDeserializer extends StdDeserializer<Object> {

    private ImmutableType immutableType;

    public ImmutableDeserializer(ImmutableType immutableType) {
        super(immutableType.getJavaClass());
        this.immutableType = immutableType;
    }

    @Override
    public Object deserialize(
            JsonParser jp,
            DeserializationContext ctx
    ) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        return Internal.produce(immutableType, null, draft -> {
            for (ImmutableProp prop : immutableType.getProps().values()) {
                if (node.has(prop.getName())) {
                    Object value = ctx.readTreeAsValue(
                            node.get(prop.getName()),
                            Utils.getJacksonType(prop)
                    );
                    ((DraftSpi)draft).__set(prop.getId(), value);
                }
            }
        });
    }
}