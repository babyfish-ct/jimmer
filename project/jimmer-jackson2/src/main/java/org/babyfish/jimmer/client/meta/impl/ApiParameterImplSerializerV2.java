package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.ApiParameter;
import org.babyfish.jimmer.client.meta.TypeRef;
import java.io.IOException;

public class ApiParameterImplSerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<ApiParameterImpl<?>> {

        @Override
        public void serialize(ApiParameterImpl<?> parameter,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeFieldName("name");
            gen.writeString(parameter.getName());
            provider.defaultSerializeField("type", parameter.getType(), gen);
            gen.writeFieldName("index");
            gen.writeNumber(parameter.getOriginalIndex());
            gen.writeEndObject();
        }
    }
