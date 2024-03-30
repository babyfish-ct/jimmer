package org.babyfish.jimmer.client.meta.impl;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.babyfish.jimmer.client.meta.ApiService;
import org.babyfish.jimmer.client.meta.Schema;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.meta.TypeName;

import java.io.IOException;
import java.util.*;

@JsonSerialize(using = SchemaImpl.Serializer.class)
@JsonDeserialize(using = SchemaImpl.Deserializer.class)
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

    public static class Serializer extends JsonSerializer<SchemaImpl<?>> {

        @Override
        public void serialize(SchemaImpl<?> schema, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            provider.defaultSerializeField("services", schema.getApiServiceMap().values(), gen);
            provider.defaultSerializeField("definitions", schema.getTypeDefinitionMap().values(), gen);
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<SchemaImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public SchemaImpl<?> deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JacksonException {
            JsonNode jsonNode = jp.getCodec().readTree(jp);
            SchemaImpl<Object> schema = new SchemaImpl<>();
            for (JsonNode serviceNode : jsonNode.get("services")) {
                ApiServiceImpl<Object> apiService = ctx.readTreeAsValue(serviceNode, ApiServiceImpl.class);
                if (Schemas.isAllowed(ctx, apiService.getGroups())) {
                    schema.addApiService(apiService);
                }
            }
            if (ctx.getAttribute(Schemas.IGNORE_DEFINITIONS) == null) {
                for (JsonNode definitionNode : jsonNode.get("definitions")) {
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
