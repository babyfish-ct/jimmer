package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.EnumConstant;
import java.io.IOException;

public class EnumConstantImplSerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<EnumConstantImpl<?>> {

        @Override
        public void serialize(EnumConstantImpl<?> constant,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            provider.defaultSerializeField("name", constant.getName(), gen);
            if (constant.getDoc() != null) {
                provider.defaultSerializeField("doc", constant.getDoc(), gen);
            }
            gen.writeEndObject();
        }
    }
