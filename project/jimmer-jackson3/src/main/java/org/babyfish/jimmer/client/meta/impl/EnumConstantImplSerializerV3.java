package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.EnumConstant;
import java.io.IOException;

public class EnumConstantImplSerializerV3 extends tools.jackson.databind.ValueSerializer<EnumConstantImpl<?>> {

        @Override
        public void serialize(EnumConstantImpl<?> constant,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeStartObject();
            ctx.defaultSerializeProperty("name", constant.getName(), gen);
            if (constant.getDoc() != null) {
                ctx.defaultSerializeProperty("doc", constant.getDoc(), gen);
            }
            gen.writeEndObject();
        }
    }
