package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.ApiParameter;
import org.babyfish.jimmer.client.meta.TypeRef;
import java.io.IOException;

public class ApiParameterImplDeserializerV3 extends tools.jackson.databind.ValueDeserializer<ApiParameterImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public ApiParameterImpl<?> deserialize(tools.jackson.core.JsonParser jp,
                                               tools.jackson.databind.DeserializationContext ctx) {
            tools.jackson.databind.JsonNode jsonNode = ctx.readTree(jp);
            ApiParameterImpl<Object> parameter = new ApiParameterImpl<>(null, jsonNode.get("name").asText());
            parameter.setType((TypeRefImpl<Object>) ctx.readTreeAsValue(jsonNode.get("type"), TypeRefImpl.class));
            parameter.setOriginalIndex(jsonNode.get("index").asInt());
            return parameter;
        }
    }
