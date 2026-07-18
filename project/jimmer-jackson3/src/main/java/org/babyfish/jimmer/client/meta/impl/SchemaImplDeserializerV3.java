package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.ApiService;
import org.babyfish.jimmer.client.meta.Schema;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.meta.TypeName;
import java.io.IOException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class SchemaImplDeserializerV3 extends tools.jackson.databind.ValueDeserializer<SchemaImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public SchemaImpl<?> deserialize(tools.jackson.core.JsonParser jp,
                                         tools.jackson.databind.DeserializationContext ctx) {
            tools.jackson.databind.JsonNode jsonNode = ctx.readTree(jp);
            SchemaImpl<Object> schema = new SchemaImpl<>();
            for (tools.jackson.databind.JsonNode serviceNode : jsonNode.get("services")) {
                ApiServiceImpl<Object> apiService = ctx.readTreeAsValue(serviceNode, ApiServiceImpl.class);
                if (Schemas.isAllowed(apiService.getGroups(), (java.util.Set<String>) ctx.getAttribute(Schemas.GROUPS))) {
                    schema.addApiService(apiService);
                }
            }
            if (ctx.getAttribute(Schemas.IGNORE_DEFINITIONS) == null) {
                for (tools.jackson.databind.JsonNode definitionNode : jsonNode.get("definitions")) {
                    TypeDefinitionImpl<Object> typeDefinition = ctx.readTreeAsValue(definitionNode, TypeDefinitionImpl.class);
                    if (Schemas.isAllowed(typeDefinition.getGroups(), (java.util.Set<String>) ctx.getAttribute(Schemas.GROUPS))) {
                        schema.addTypeDefinition(typeDefinition);
                    }
                }
            }
            return schema;
        }
    }
