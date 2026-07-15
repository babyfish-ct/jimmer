package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.*;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ApiOperationImplDeserializerV3 extends tools.jackson.databind.ValueDeserializer<ApiOperationImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public ApiOperationImpl<?> deserialize(tools.jackson.core.JsonParser jp,
                                               tools.jackson.databind.DeserializationContext ctx) {
            tools.jackson.databind.JsonNode jsonNode = ctx.readTree(jp);
            String name = jsonNode.get("name").asText();
            ApiOperationImpl<Object> operation = new ApiOperationImpl<>(null, name);
            operation.setKey(jsonNode.get("key").asText());
            if (jsonNode.has("groups")) {
                operation.setGroups(
                        Collections.unmodifiableList(
                                ctx.readTreeAsValue(jsonNode.get("groups"),
                                        ctx.getTypeFactory().constructCollectionType(List.class, String.class))
                        )
                );
                if (!Schemas.isAllowed(operation.getGroups(), (java.util.Set<String>) ctx.getAttribute(Schemas.GROUPS))) {
                    return operation;
                }
            }
            if (jsonNode.has("doc")) {
                operation.setDoc(ctx.readTreeAsValue(jsonNode.get("doc"), Doc.class));
            }
            if (jsonNode.has("parameters")) {
                for (tools.jackson.databind.JsonNode paramNode : jsonNode.get("parameters")) {
                    ApiParameterImpl<Object> parameter = ctx.readTreeAsValue(paramNode, ApiParameterImpl.class);
                    operation.addParameter(parameter);
                }
            }
            if (jsonNode.has("returnType")) {
                operation.setReturnType(ctx.readTreeAsValue(jsonNode.get("returnType"), TypeRefImpl.class));
            }
            if (jsonNode.has("exceptions")) {
                List<TypeName> typeNames = ctx.readTreeAsValue(jsonNode.get("exceptions"),
                        ctx.getTypeFactory().constructCollectionType(List.class, TypeName.class));
                operation.setExceptionTypeNames(typeNames);
            }
            return operation;
        }
    }
