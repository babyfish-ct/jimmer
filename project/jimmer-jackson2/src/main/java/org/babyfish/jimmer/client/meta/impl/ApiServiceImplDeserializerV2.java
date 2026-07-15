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

public class ApiServiceImplDeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<ApiServiceImpl<?>> {

        private static final com.fasterxml.jackson.databind.type.CollectionType GROUPS_TYPE =
                com.fasterxml.jackson.databind.type.CollectionType.construct(
                        List.class,
                        null,
                        null,
                        null,
                        com.fasterxml.jackson.databind.type.SimpleType.constructUnsafe(String.class)
                );

        @SuppressWarnings("unchecked")
        @Override
        public ApiServiceImpl<?> deserialize(com.fasterxml.jackson.core.JsonParser jp,
                                             com.fasterxml.jackson.databind.DeserializationContext ctx) throws IOException {
            com.fasterxml.jackson.databind.JsonNode jsonNode = jp.getCodec().readTree(jp);
            ApiServiceImpl<Object> service = new ApiServiceImpl<>(
                    null,
                    ctx.readTreeAsValue(jsonNode.get("typeName"), TypeName.class)
            );
            if (jsonNode.has("groups")) {
                service.setGroups(
                        Collections.unmodifiableList(
                                ctx.readTreeAsValue(jsonNode.get("groups"), GROUPS_TYPE)
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
                for (com.fasterxml.jackson.databind.JsonNode operationNode : jsonNode.get("operations")) {
                    ApiOperationImpl<Object> operation = ctx.readTreeAsValue(operationNode, ApiOperationImpl.class);
                    if (Schemas.isAllowed(operation.getGroups(), (java.util.Set<String>) ctx.getAttribute(Schemas.GROUPS))) {
                        service.addOperation(operation);
                    }
                }
            }
            return service;
        }
    }
