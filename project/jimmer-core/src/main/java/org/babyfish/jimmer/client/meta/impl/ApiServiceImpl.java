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

@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ApiServiceImpl.SerializerV2.class)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ApiServiceImpl.DeserializerV2.class)
@tools.jackson.databind.annotation.JsonSerialize(using = ApiServiceImpl.SerializerV3.class)
@tools.jackson.databind.annotation.JsonDeserialize(using = ApiServiceImpl.DeserializerV3.class)
public class ApiServiceImpl<S> extends AstNode<S> implements ApiService {

    private TypeName typeName;

    private List<String> groups;

    private List<ApiOperationImpl<S>> operations = new ArrayList<>();

    private Doc doc;

    ApiServiceImpl(S source, TypeName typeName) {
        super(source);
        this.typeName = typeName;
    }

    @Override
    public TypeName getTypeName() {
        return typeName;
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

    @Nullable
    @Override
    public ApiOperation findOperation(String name, Parameter... parameters) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(name);
        for (Parameter parameter : parameters) {
            keyBuilder.append(':');
            if (parameter.isAnnotationPresent(ApiIgnore.class)) {
                continue;
            }
            Class<?> type = parameter.getType();
            if (type.isArray()) {
                keyBuilder.append(List.class.getName());
            } else if (Collection.class.isAssignableFrom(type)) {
                keyBuilder.append(List.class.getName());
            } else {
                keyBuilder.append(fullName(type));
            }
        }
        String key = keyBuilder.toString();
        for (ApiOperationImpl<S> operation : operations) {
            if (operation.key().equals(key)) {
                return operation;
            }
        }
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
    public void accept(AstNodeVisitor<S> visitor) {
        try {
            if (!visitor.visitAstNode(this)) {
                return;
            }
            for (ApiOperationImpl<S> operation : operations) {
                operation.accept(visitor);
            }
        } finally {
            visitor.visitedAstNode(this);
        }
    }

    @Override
    public String toString() {
        return typeName.toString();
    }

    @com.fasterxml.jackson.annotation.JsonValue
    public String value() {
        return "<api>";
    }

    static class SerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<ApiServiceImpl<?>> {

        @Override
        public void serialize(ApiServiceImpl<?> service,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            provider.defaultSerializeField("typeName", service.getTypeName(), gen);
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

    static class DeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<ApiServiceImpl<?>> {

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
                if (!Schemas.isAllowed(ctx, service.getGroups())) {
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
                    if (Schemas.isAllowed(ctx, operation.getGroups())) {
                        service.addOperation(operation);
                    }
                }
            }
            return service;
        }
    }

    static class SerializerV3 extends tools.jackson.databind.ValueSerializer<ApiServiceImpl<?>> {

        @Override
        public void serialize(ApiServiceImpl<?> service,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeStartObject();
            ctx.defaultSerializeProperty("typeName", service.getTypeName(), gen);
            if (service.getGroups() != null) {
                ctx.defaultSerializeProperty("groups", service.getGroups(), gen);
            }
            if (service.getDoc() != null) {
                ctx.defaultSerializeProperty("doc", service.getDoc(), gen);
            }
            if (!service.getOperations().isEmpty()) {
                ctx.defaultSerializeProperty("operations", service.getOperations(), gen);
            }
            gen.writeEndObject();
        }
    }

    static class DeserializerV3 extends tools.jackson.databind.ValueDeserializer<ApiServiceImpl<?>> {

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
                if (!Schemas.isAllowed(ctx, service.getGroups())) {
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
                    if (Schemas.isAllowed(ctx, operation.getGroups())) {
                        service.addOperation(operation);
                    }
                }
            }
            return service;
        }
    }

    private static String fullName(Class<?> type) {
        Class<?> declaringClass = type.getDeclaringClass();
        if (declaringClass != null) {
            return fullName(declaringClass) + '.' + type.getSimpleName();
        }
        return type.getName();
    }
}