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

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonSerialize(using = SchemaImpl.Serializer.class)
@JsonDeserialize(using = SchemaImpl.Deserializer.class)
public class SchemaImpl<S> extends AstNode<S> implements Schema {

    private List<ApiServiceImpl<S>> apiServices = new ArrayList<>();

    private Map<String, TypeDefinitionImpl<S>> typeDefinitionMap = new LinkedHashMap<>();

    SchemaImpl() {
        super(null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ApiService> getApiServices() {
        return (List<ApiService>) (List<?>) apiServices;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, TypeDefinition> getTypeDefinitionMap() {
        return (Map<String, TypeDefinition>) (Map<?, ?>) typeDefinitionMap;
    }

    public void addApiService(ApiServiceImpl<S> apiService) {
        this.apiServices.add(apiService);
    }

    public void addTypeDefinition(TypeDefinitionImpl<S> typeDefinition) {
        this.typeDefinitionMap.put(typeDefinition.getTypeName(), typeDefinition);
    }

    @Override
    public void accept(TypeNameVisitor visitor) {
        for (ApiServiceImpl<S> apiService : apiServices) {
            apiService.accept(visitor);
        }
        // Cannot visit type definitions because the current visitor is used create or mark definitions
    }

    @Override
    public String toString() {
        return "SchemaImpl{" +
                "apiServices=" + apiServices +
                ", typeDefinitionMap=" + typeDefinitionMap +
                '}';
    }

    public static class Serializer extends JsonSerializer<SchemaImpl<?>> {

        @Override
        public void serialize(SchemaImpl<?> schema, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            provider.defaultSerializeField("services", schema.getApiServices(), gen);
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
                schema.addApiService(ctx.readTreeAsValue(serviceNode, ApiServiceImpl.class));
            }
            for (JsonNode definitionNode : jsonNode.get("definitions")) {
                schema.addTypeDefinition(ctx.readTreeAsValue(definitionNode, TypeDefinitionImpl.class));
            }
            return schema;
        }
    }
}
