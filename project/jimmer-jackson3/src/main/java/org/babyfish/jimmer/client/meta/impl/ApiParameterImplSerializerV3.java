package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.ApiParameter;
import org.babyfish.jimmer.client.meta.TypeRef;
import java.io.IOException;

public class ApiParameterImplSerializerV3 extends tools.jackson.databind.ValueSerializer<ApiParameterImpl<?>> {

        @Override
        public void serialize(ApiParameterImpl<?> parameter,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeStartObject();
            gen.writeName("name");
            gen.writeString(parameter.getName());
            ctx.defaultSerializeProperty("type", parameter.getType(), gen);
            gen.writeName("index");
            gen.writeNumber(parameter.getOriginalIndex());
            gen.writeEndObject();
        }
    }
