package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.meta.TypeRef;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TypeRefImplDeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<TypeRefImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public TypeRefImpl<?> deserialize(com.fasterxml.jackson.core.JsonParser jp,
                                          com.fasterxml.jackson.databind.DeserializationContext ctx) throws IOException {
            com.fasterxml.jackson.databind.JsonNode jsonNode = jp.getCodec().readTree(jp);
            TypeRefImpl<Object> typeRef = new TypeRefImpl<>();
            typeRef.setTypeName(ctx.readTreeAsValue(jsonNode.get("typeName"), TypeName.class));
            if (jsonNode.has("nullable")) {
                typeRef.setNullable(jsonNode.get("nullable").asBoolean());
            }
            if (jsonNode.has("arguments")) {
                for (com.fasterxml.jackson.databind.JsonNode argNode : jsonNode.get("arguments")) {
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
