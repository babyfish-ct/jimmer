package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.meta.TypeRef;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TypeRefImplDeserializerV3 extends tools.jackson.databind.ValueDeserializer<TypeRefImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public TypeRefImpl<?> deserialize(tools.jackson.core.JsonParser jp,
                                          tools.jackson.databind.DeserializationContext ctx) {
            tools.jackson.databind.JsonNode jsonNode = ctx.readTree(jp);
            TypeRefImpl<Object> typeRef = new TypeRefImpl<>();
            typeRef.setTypeName(ctx.readTreeAsValue(jsonNode.get("typeName"), TypeName.class));
            if (jsonNode.has("nullable")) {
                typeRef.setNullable(jsonNode.get("nullable").asBoolean());
            }
            if (jsonNode.has("arguments")) {
                for (tools.jackson.databind.JsonNode argNode : jsonNode.get("arguments")) {
                    TypeRefImpl<Object> arg = (TypeRefImpl<Object>) ctx.readTreeAsValue(argNode, TypeRefImpl.class);
                    typeRef.addArgument(arg);
                }
            }
            if (jsonNode.has("fetchBy")) {
                typeRef.setFetchBy(jsonNode.get("fetchBy").asText());
                typeRef.setFetcherOwner(TypeName.parse(jsonNode.get("fetcherOwner").asText()));
                if (jsonNode.has("fetcherDoc")) {
                    typeRef.setFetcherDoc(ctx.readTreeAsValue(jsonNode.get("fetcherDoc"), Doc.class));
                }
            }
            return typeRef;
        }
    }
