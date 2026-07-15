package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.EnumConstant;
import java.io.IOException;

public class EnumConstantImplDeserializerV3 extends tools.jackson.databind.ValueDeserializer<EnumConstantImpl<?>> {

        @Override
        public EnumConstantImpl<?> deserialize(tools.jackson.core.JsonParser jp,
                                               tools.jackson.databind.DeserializationContext ctx) {
            tools.jackson.databind.JsonNode jsonNode = ctx.readTree(jp);
            EnumConstantImpl<Object> constant = new EnumConstantImpl<>(null, jsonNode.get("name").asText());
            constant.setDoc(ctx.readTreeAsValue(jsonNode.get("doc"), Doc.class));
            return constant;
        }
    }
