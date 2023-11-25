package org.babyfish.jimmer.client.meta.impl;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.client.meta.ApiService;
import org.babyfish.jimmer.client.meta.ApiOperation;
import org.babyfish.jimmer.client.meta.Doc;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonSerialize(using = ApiServiceImpl.Serializer.class)
@JsonDeserialize(using = ApiServiceImpl.Deserializer.class)
public class ApiServiceImpl<S> extends AstNode<S> implements ApiService {

    private String typeName;

    private List<String> groups;

    private List<ApiOperationImpl<S>> operations = new ArrayList<>();

    private Doc doc;

    ApiServiceImpl(S source, String typeName) {
        super(source);
        this.typeName = typeName;
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    @Nullable
    @Override
    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        if (groups == null || groups.isEmpty()) {
            this.groups = null;
        } else {
            this.groups = Collections.unmodifiableList(groups);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ApiOperation> getOperations() {
        return (List<ApiOperation>) (List<?>) operations;
    }

    @Override
    public ApiOperation findOperation(String name, Class<?>... types) {
        return null;
    }

    @Override
    public ApiOperation findOperation(String name, String... typeNames) {
        return null;
    }

    public void addOperation(ApiOperationImpl<S> operation) {
        this.operations.add(operation);
    }

    @Nullable
    @Override
    public Doc getDoc() {
        return doc;
    }

    public void setDoc(Doc doc) {
        this.doc = doc;
    }

    @Override
    public void accept(TypeNameVisitor visitor) {
        for (ApiOperationImpl<S> operation : operations) {
            operation.accept(visitor);
        }
    }

    @Override
    public String toString() {
        return "ApiServiceImpl{" +
                "className='" + typeName + '\'' +
                ", groups='" + groups + '\'' +
                ", operations=" + operations +
                ", doc='" + doc + '\'' +
                '}';
    }

    @JsonValue
    public String value() {
        return "<api>";
    }

    public static class Serializer extends JsonSerializer<ApiServiceImpl<?>> {

        @Override
        public void serialize(ApiServiceImpl<?> service, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeFieldName("typeName");
            gen.writeString(service.getTypeName());
            if (service.getGroups() != null) {
                provider.defaultSerializeField("groups", service.getGroups(), gen);
            }
            if (service.getDoc() != null) {
                provider.defaultSerializeField("doc", service.getDoc(), gen);
            }
            if (!service.getOperations().isEmpty()) {
                provider.defaultSerializeField("operations", service.getOperations(), gen);
            }
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ApiServiceImpl<?>> {

        private static final CollectionType GROUPS_TYPE = CollectionType.construct(
                List.class,
                null,
                null,
                null,
                SimpleType.constructUnsafe(String.class)
        );

        @SuppressWarnings("unchecked")
        @Override
        public ApiServiceImpl<?> deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JacksonException {
            JsonNode jsonNode = jp.getCodec().readTree(jp);
            ApiServiceImpl<Object> service = new ApiServiceImpl<>(null, jsonNode.get("typeName").asText());
            if (jsonNode.has("groups")) {
                service.setGroups(
                        Collections.unmodifiableList(
                                ctx.readTreeAsValue(jsonNode.get("groups"), GROUPS_TYPE)
                        )
                );
            }
            if (jsonNode.has("doc")) {
                service.setDoc(
                        ctx.readTreeAsValue(jsonNode.get("doc"), Doc.class)
                );
            }
            if (jsonNode.has("operations")) {
                for (JsonNode operationNode : jsonNode.get("operations")) {
                    service.addOperation(ctx.readTreeAsValue(operationNode, ApiOperationImpl.class));
                }
            }
            return service;
        }
    }
}
