package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.core.json.PackageVersion;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TreeTraversingParser;
import org.babyfish.jimmer.jackson.meta.BeanProps;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;

import java.io.IOException;

public class ImmutableDeserializer extends StdDeserializer<Object> {

    private final ImmutableType immutableType;

    public ImmutableDeserializer(ImmutableType immutableType) {
        super(immutableType.getJavaClass());
        this.immutableType = immutableType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object deserialize(
            JsonParser jp,
            DeserializationContext ctx
    ) throws IOException {
        JsonNode node = jp.getCodec().readTree(jp);

        return Internal.produce(immutableType, null, draft -> {
            for (ImmutableProp prop : immutableType.getProps().values()) {
                if (node.has(prop.getName())) {
                    Object value = PropDeserializeUtils.readTreeAsValue(
                            ctx,
                            node.get(prop.getName()),
                            BeanProps.get(ctx.getTypeFactory(), prop)
                    );
                    if (value != null && prop.getConverter() != null) {
                        value = ((Converter<Object>)prop.getConverter()).input(value);
                    }
                    ((DraftSpi)draft).__set(prop.getId(), value);
                }
            }
        });
    }
}