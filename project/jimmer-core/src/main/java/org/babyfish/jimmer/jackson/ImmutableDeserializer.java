package org.babyfish.jimmer.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.babyfish.jimmer.jackson.meta.BeanProps;
import org.babyfish.jimmer.meta.ImmutableProp;
import org.babyfish.jimmer.meta.ImmutableType;
import org.babyfish.jimmer.runtime.DraftSpi;
import org.babyfish.jimmer.runtime.Internal;

import java.io.IOException;

public class ImmutableDeserializer extends StdDeserializer<Object> {

    private final ImmutableType immutableType;

    private final PropNameConverter propNameConverter;

    public ImmutableDeserializer(ImmutableType immutableType, PropNameConverter propNameConverter) {
        super(immutableType.getJavaClass());
        this.immutableType = immutableType;
        this.propNameConverter = propNameConverter;
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
                if (prop.isMutable()) {
                    String fieldName = propNameConverter.fieldName(prop);
                    String nodeName = null;
                    if (node.has(fieldName)) {
                        nodeName = fieldName;
                    } else {
                        for (String alias : propNameConverter.aliases(prop)) {
                            if (node.has(alias)) {
                                nodeName = alias;
                                break;
                            }
                        }
                    }
                    if (nodeName != null) {
                        Object value = PropDeserializeUtils.readTreeAsValue(
                                ctx,
                                node.get(nodeName),
                                BeanProps.get(ctx.getTypeFactory(), prop)
                        );
                        if (value != null && prop.getConverter() != null) {
                            value = ((Converter<Object>) prop.getConverter()).input(value);
                        }
                        ((DraftSpi) draft).__set(prop.getId(), value);
                    }
                }
            }
        });
    }
}