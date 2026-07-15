package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.ApiParameter;
import org.babyfish.jimmer.client.meta.TypeRef;
import java.io.IOException;

public class ApiParameterImplDeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<ApiParameterImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public ApiParameterImpl<?> deserialize(com.fasterxml.jackson.core.JsonParser jp,
                                               com.fasterxml.jackson.databind.DeserializationContext ctx) throws IOException {
            com.fasterxml.jackson.databind.JsonNode jsonNode = jp.getCodec().readTree(jp);
            ApiParameterImpl<Object> parameter = new ApiParameterImpl<>(null, jsonNode.get("name").asText());
            parameter.setType((TypeRefImpl<Object>) ctx.readTreeAsValue(jsonNode.get("type"), TypeRefImpl.class));
            parameter.setOriginalIndex(jsonNode.get("index").asInt());
            return parameter;
        }
    }
