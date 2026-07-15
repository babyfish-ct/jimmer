package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.Prop;
import org.babyfish.jimmer.client.meta.TypeRef;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.Properties;

public class PropImplDeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<PropImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public PropImpl<?> deserialize(com.fasterxml.jackson.core.JsonParser jp,
                                       com.fasterxml.jackson.databind.DeserializationContext ctx) throws IOException {
            com.fasterxml.jackson.databind.JsonNode jsonNode = jp.getCodec().readTree(jp);
            PropImpl<Object> prop = new PropImpl<>(null, jsonNode.get("name").asText());
            prop.setType(ctx.readTreeAsValue(jsonNode.get("type"), TypeRefImpl.class));
            if (jsonNode.has("doc")) {
                prop.setDoc(ctx.readTreeAsValue(jsonNode.get("doc"), Doc.class));
            }
            return prop;
        }
    }
