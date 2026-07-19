package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.ApiService;
import org.babyfish.jimmer.client.meta.Schema;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.meta.TypeName;

import java.io.IOException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = SchemaImpl.SerializerV2.class)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = SchemaImpl.DeserializerV2.class)
@tools.jackson.databind.annotation.JsonSerialize(using = SchemaImpl.SerializerV3.class)
@tools.jackson.databind.annotation.JsonDeserialize(using = SchemaImpl.DeserializerV3.class)
public class SchemaImpl<S> extends AstNode<S> implements Schema {

    private Map<TypeName, ApiServiceImpl<S>> apiServiceMap;

    private Map<TypeName, TypeDefinitionImpl<S>> typeDefinitionMap = new TreeMap<>();

    public SchemaImpl() {
        this(null);
    }

    public SchemaImpl(Map<TypeName, ApiServiceImpl<S>> apiServiceMap) {
        super(null);
        this.apiServiceMap = apiServiceMap != null ?
                apiServiceMap instanceof NavigableMap<?, ?> ? apiServiceMap : new TreeMap<>(apiServiceMap) :
                new TreeMap<>();
    }

    public SchemaImpl(Map<TypeName, ApiServiceImpl<S>> apiServiceMap, Map<TypeName, TypeDefinitionImpl<S>> typeDefinitionMap) {
        this.apiServiceMap = apiServiceMap;
        this.typeDefinitionMap = typeDefinitionMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<TypeName, ApiService> getApiServiceMap() {
        return (Map<TypeName, ApiService>) (Map<?, ?>) apiServiceMap;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<TypeName, TypeDefinition> getTypeDefinitionMap() {
        return (Map<TypeName, TypeDefinition>) (Map<?, ?>) typeDefinitionMap;
    }

    public void addApiService(ApiServiceImpl<S> apiService) {
        this.apiServiceMap.put(apiService.getTypeName(), apiService);
    }

    public void addTypeDefinition(TypeDefinitionImpl<S> typeDefinition) {
        this.typeDefinitionMap.put(typeDefinition.getTypeName(), typeDefinition);
    }

    @Override
    public void accept(AstNodeVisitor<S> visitor) {
        try {
            if (!visitor.visitAstNode(this)) {
                return;
            }
            for (ApiServiceImpl<S> apiService : apiServiceMap.values()) {
                apiService.accept(visitor);
            }
            // Cannot visit type definitions because the current visitor is used create or mark definitions
        } finally {
            visitor.visitedAstNode(this);
        }
    }

    @Override
    public String toString() {
        return "SchemaImpl{" +
                "apiServiceMap=" + apiServiceMap +
                ", typeDefinitionMap=" + typeDefinitionMap +
                '}';
    }

    static class SerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<SchemaImpl<?>> {

        @Override
        public void serialize(SchemaImpl<?> schema,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            provider.defaultSerializeField("services", schema.getApiServiceMap().values(), gen);
            provider.defaultSerializeField("definitions", schema.getTypeDefinitionMap().values(), gen);
            gen.writeEndObject();
        }
    }

    static class DeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<SchemaImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public SchemaImpl<?> deserialize(com.fasterxml.jackson.core.JsonParser jp,
                                         com.fasterxml.jackson.databind.DeserializationContext ctx) throws IOException {
            com.fasterxml.jackson.databind.JsonNode jsonNode = jp.getCodec().readTree(jp);
            SchemaImpl<Object> schema = new SchemaImpl<>();
            for (com.fasterxml.jackson.databind.JsonNode serviceNode : jsonNode.get("services")) {
                ApiServiceImpl<Object> apiService = ctx.readTreeAsValue(serviceNode, ApiServiceImpl.class);
                if (Schemas.isAllowed(ctx, apiService.getGroups())) {
                    schema.addApiService(apiService);
                }
            }
            if (ctx.getAttribute(Schemas.IGNORE_DEFINITIONS) == null) {
                for (com.fasterxml.jackson.databind.JsonNode definitionNode : jsonNode.get("definitions")) {
                    TypeDefinitionImpl<Object> typeDefinition = ctx.readTreeAsValue(definitionNode, TypeDefinitionImpl.class);
                    if (Schemas.isAllowed(ctx, typeDefinition.getGroups())) {
                        schema.addTypeDefinition(typeDefinition);
                    }
                }
            }
            return schema;
        }
    }

    static class SerializerV3 extends tools.jackson.databind.ValueSerializer<SchemaImpl<?>> {

        @Override
        public void serialize(SchemaImpl<?> schema,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeStartObject();
            ctx.defaultSerializeProperty("services", schema.getApiServiceMap().values(), gen);
            ctx.defaultSerializeProperty("definitions", schema.getTypeDefinitionMap().values(), gen);
            gen.writeEndObject();
        }
    }

    static class DeserializerV3 extends tools.jackson.databind.ValueDeserializer<SchemaImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public SchemaImpl<?> deserialize(tools.jackson.core.JsonParser jp,
                                         tools.jackson.databind.DeserializationContext ctx) {
            tools.jackson.databind.JsonNode jsonNode = ctx.readTree(jp);
            SchemaImpl<Object> schema = new SchemaImpl<>();
            for (tools.jackson.databind.JsonNode serviceNode : jsonNode.get("services")) {
                ApiServiceImpl<Object> apiService = ctx.readTreeAsValue(serviceNode, ApiServiceImpl.class);
                if (Schemas.isAllowed(ctx, apiService.getGroups())) {
                    schema.addApiService(apiService);
                }
            }
            if (ctx.getAttribute(Schemas.IGNORE_DEFINITIONS) == null) {
                for (tools.jackson.databind.JsonNode definitionNode : jsonNode.get("definitions")) {
                    TypeDefinitionImpl<Object> typeDefinition = ctx.readTreeAsValue(definitionNode, TypeDefinitionImpl.class);
                    if (Schemas.isAllowed(ctx, typeDefinition.getGroups())) {
                        schema.addTypeDefinition(typeDefinition);
                    }
                }
            }
            return schema;
        }
    }
}
