package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.Prop;
import org.babyfish.jimmer.client.meta.TypeRef;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.Properties;

public class PropImplSerializerV3 extends tools.jackson.databind.ValueSerializer<PropImpl<?>> {

        @Override
        public void serialize(PropImpl<?> prop,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeStartObject();
            gen.writeName("name");
            gen.writeString(prop.getName());
            ctx.defaultSerializeProperty("type", prop.getType(), gen);
            if (prop.getDoc() != null) {
                ctx.defaultSerializeProperty("doc", prop.getDoc(), gen);
            }
            gen.writeEndObject();
        }
    }
