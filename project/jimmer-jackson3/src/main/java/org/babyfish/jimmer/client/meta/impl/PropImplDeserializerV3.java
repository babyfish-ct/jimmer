package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.Prop;
import org.babyfish.jimmer.client.meta.TypeRef;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.Properties;

public class PropImplDeserializerV3 extends tools.jackson.databind.ValueDeserializer<PropImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public PropImpl<?> deserialize(tools.jackson.core.JsonParser jp,
                                       tools.jackson.databind.DeserializationContext ctx) {
            tools.jackson.databind.JsonNode jsonNode = ctx.readTree(jp);
            PropImpl<Object> prop = new PropImpl<>(null, jsonNode.get("name").asText());
            prop.setType(ctx.readTreeAsValue(jsonNode.get("type"), TypeRefImpl.class));
            if (jsonNode.has("doc")) {
                prop.setDoc(ctx.readTreeAsValue(jsonNode.get("doc"), Doc.class));
            }
            return prop;
        }
    }
