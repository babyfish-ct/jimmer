package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.Prop;
import org.babyfish.jimmer.client.meta.TypeRef;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.Properties;

public class PropImplSerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<PropImpl<?>> {

        @Override
        public void serialize(PropImpl<?> prop,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeFieldName("name");
            gen.writeString(prop.getName());
            provider.defaultSerializeField("type", prop.getType(), gen);
            if (prop.getDoc() != null) {
                provider.defaultSerializeField("doc", prop.getDoc(), gen);
            }
            gen.writeEndObject();
        }
    }
