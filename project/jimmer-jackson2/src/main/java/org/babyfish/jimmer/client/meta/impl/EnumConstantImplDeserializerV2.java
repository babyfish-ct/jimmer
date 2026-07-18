package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.EnumConstant;
import java.io.IOException;

public class EnumConstantImplDeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<EnumConstantImpl<?>> {

        @Override
        public EnumConstantImpl<?> deserialize(com.fasterxml.jackson.core.JsonParser jp,
                                               com.fasterxml.jackson.databind.DeserializationContext ctx) throws IOException {
            com.fasterxml.jackson.databind.JsonNode jsonNode = jp.getCodec().readTree(jp);
            EnumConstantImpl<Object> constant = new EnumConstantImpl<>(null, jsonNode.get("name").asText());
            constant.setDoc(ctx.readTreeAsValue(jsonNode.get("doc"), Doc.class));
            return constant;
        }
    }
