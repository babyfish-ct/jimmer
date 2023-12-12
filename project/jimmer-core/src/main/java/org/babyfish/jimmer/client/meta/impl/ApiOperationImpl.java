package org.babyfish.jimmer.client.meta.impl;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.client.meta.*;
import org.jetbrains.annotations.Nullable;

import javax.lang.model.element.Element;
import java.io.IOException;
import java.util.*;

@JsonSerialize(using = ApiOperationImpl.Serializer.class)
@JsonDeserialize(using = ApiOperationImpl.Deserializer.class)
public class ApiOperationImpl<S> extends AstNode<S> implements ApiOperation {

    private String name;

    private List<String> groups;

    private final List<ApiParameterImpl<S>> parameters = new ArrayList<>();

    private TypeRefImpl<S> returnType;

    private List<TypeRefImpl<S>> exceptionTypes = Collections.emptyList();

    private Doc doc;

    private StringBuilder keyBuilder;

    private String key;

    ApiOperationImpl(S source, String name) {
        super(source);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

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
    public List<ApiParameter> getParameters() {
        return (List<ApiParameter>) (List<?>) parameters;
    }

    public void addParameter(ApiParameterImpl<S> parameter) {
        this.parameters.add(parameter);
        addIgnoredParameter(parameter);
    }

    public void addIgnoredParameter(ApiParameterImpl<S> parameter) {
        if (keyBuilder == null) {
            keyBuilder = new StringBuilder();
            keyBuilder.append(name);
        }
        TypeName typeName = parameter.getType().getTypeName();
        if (typeName.getTypeVariable() != null) {
            throw new AssertionError(
                    "Illegal parameter \"" + parameter.getName() + "\", its type cannot be type variable"
            );
        }
        keyBuilder.append(':').append(typeName);
    }

    @Nullable
    @Override
    public TypeRef getReturnType() {
        return returnType;
    }

    public void setReturnType(TypeRefImpl<S> returnType) {
        this.returnType = returnType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<TypeRef> getExceptionTypes() {
        return (List<TypeRef>) (List<?>) exceptionTypes;
    }

    public void setExceptionTypeNames(Collection<TypeName> exceptionTypeNames) {
        if (exceptionTypeNames.isEmpty()) {
            this.exceptionTypes = Collections.emptyList();
        }
        List<TypeRefImpl<S>> typeRefs = new ArrayList<>(exceptionTypeNames.size());
        for (TypeName exceptionTypeName : exceptionTypeNames) {
            TypeRefImpl<S> typeRef = new TypeRefImpl<>();
            typeRef.setTypeName(exceptionTypeName);
            typeRefs.add(typeRef);
        }
        this.exceptionTypes = Collections.unmodifiableList(typeRefs);
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
        visitor.visitAstNode(this);
        try {
            for (ApiParameterImpl<S> parameter : parameters) {
                parameter.accept(visitor);
            }
            if (returnType != null) {
                returnType.accept(visitor);
            }
            for (TypeRefImpl<S> typeRef : exceptionTypes) {
                typeRef.accept(visitor);
            }
        } finally {
            visitor.visitedAstNode(this);
        }
    }

    @Override
    public String toString() {
        return key().replaceFirst(":", "(").replace(":", ", ") + ')';
    }

    public String key() {
        String key = this.key;
        if (key == null) {
            if (keyBuilder == null) {
                this.key = key = name;
            } else {
                this.key = key = keyBuilder.toString();
                keyBuilder = null;
            }
        }
        return key;
    }

    public void setKey(String key) {
        this.key = key;
        this.keyBuilder = null;
    }

    public static class Serializer extends JsonSerializer<ApiOperationImpl<?>> {

        @Override
        public void serialize(ApiOperationImpl<?> operation, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeFieldName("name");
            gen.writeString(operation.getName());
            gen.writeFieldName("key");
            gen.writeString(operation.key());
            if (operation.getGroups() != null) {
                provider.defaultSerializeField("groups", operation.getGroups(), gen);
            }
            if (operation.getDoc() != null) {
                provider.defaultSerializeField("doc", operation.getDoc(), gen);
            }
            if (!operation.getParameters().isEmpty()) {
                provider.defaultSerializeField("parameters", operation.getParameters(), gen);
            }
            if (operation.getReturnType() != null) {
                provider.defaultSerializeField("returnType", operation.getReturnType(), gen);
            }
            if (!operation.getExceptionTypes().isEmpty()) {
                gen.writeFieldName("exceptions");
                gen.writeStartArray();
                for (TypeRef exceptionType : operation.getExceptionTypes()) {
                    provider.defaultSerializeValue(exceptionType.getTypeName(), gen);
                }
                gen.writeEndArray();
            }
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ApiOperationImpl<?>> {

        private static final JavaType TYPE_NAME_LIST_TYPE =
                CollectionType.construct(
                        List.class,
                        null,
                        null,
                        null,
                        SimpleType.constructUnsafe(TypeName.class)
                );

        private static final CollectionType GROUPS_TYPE = CollectionType.construct(
                List.class,
                null,
                null,
                null,
                SimpleType.constructUnsafe(String.class)
        );

        @SuppressWarnings("unchecked")
        @Override
        public ApiOperationImpl<?> deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JacksonException {
            JsonNode jsonNode = jp.getCodec().readTree(jp);
            String name = jsonNode.get("name").asText();
            ApiOperationImpl<Object> operation = new ApiOperationImpl<>(null, name);
            operation.setKey(jsonNode.get("key").asText());
            if (jsonNode.has("groups")) {
                operation.setGroups(
                        Collections.unmodifiableList(
                            ctx.readTreeAsValue(jsonNode.get("groups"), GROUPS_TYPE)
                        )
                );
                if (!Schemas.isAllowed(ctx, operation.getGroups())) {
                    return operation;
                }
            }
            if (jsonNode.has("doc")) {
                operation.setDoc(ctx.readTreeAsValue(jsonNode.get("doc"), Doc.class));
            }
            if (jsonNode.has("parameters")) {
                for (JsonNode paramNode : jsonNode.get("parameters")) {
                    ApiParameterImpl<Object> parameter = ctx.readTreeAsValue(paramNode, ApiParameterImpl.class);
                    operation.addParameter(parameter);
                }
            }
            if (jsonNode.has("returnType")) {
                operation.setReturnType(ctx.readTreeAsValue(jsonNode.get("returnType"), TypeRefImpl.class));
            }
            if (jsonNode.has("exceptions")) {
                List<TypeName> typeNames = ctx.readTreeAsValue(jsonNode.get("exceptions"), TYPE_NAME_LIST_TYPE);
                operation.setExceptionTypeNames(typeNames);
            }
            return operation;
        }
    }
}
