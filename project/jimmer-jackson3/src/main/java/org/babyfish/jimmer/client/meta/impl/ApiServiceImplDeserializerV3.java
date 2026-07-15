package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.ApiIgnore;
import org.babyfish.jimmer.client.meta.ApiOperation;
import org.babyfish.jimmer.client.meta.ApiService;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.TypeName;
import org.jetbrains.annotations.Nullable;
import java.io.IOException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ApiServiceImplDeserializerV3 extends tools.jackson.databind.ValueDeserializer<ApiServiceImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public ApiServiceImpl<?> deserialize(tools.jackson.core.JsonParser jp,
                                             tools.jackson.databind.DeserializationContext ctx) {
            tools.jackson.databind.JsonNode jsonNode = ctx.readTree(jp);
            ApiServiceImpl<Object> service = new ApiServiceImpl<>(
                    null,
                    ctx.readTreeAsValue(jsonNode.get("typeName"), TypeName.class)
            );
            if (jsonNode.has("groups")) {
                service.setGroups(
                        Collections.unmodifiableList(
                                ctx.readTreeAsValue(jsonNode.get("groups"),
                                        ctx.getTypeFactory().constructCollectionType(List.class, String.class))
                        )
                );
                if (!Schemas.isAllowed(service.getGroups(), (java.util.Set<String>) ctx.getAttribute(Schemas.GROUPS))) {
                    return service;
                }
            }
            if (jsonNode.has("doc")) {
                service.setDoc(
                        ctx.readTreeAsValue(jsonNode.get("doc"), Doc.class)
                );
            }
            if (jsonNode.has("operations")) {
                for (tools.jackson.databind.JsonNode operationNode : jsonNode.get("operations")) {
                    ApiOperationImpl<Object> operation = ctx.readTreeAsValue(operationNode, ApiOperationImpl.class);
                    if (Schemas.isAllowed(operation.getGroups(), (java.util.Set<String>) ctx.getAttribute(Schemas.GROUPS))) {
                        service.addOperation(operation);
                    }
                }
            }
            return service;
        }
    }
