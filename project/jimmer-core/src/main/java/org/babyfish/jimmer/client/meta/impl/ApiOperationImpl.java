package org.babyfish.jimmer.client.meta.impl;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.MapType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.client.meta.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

@JsonSerialize(using = ApiOperationImpl.Serializer.class)
@JsonDeserialize(using = ApiOperationImpl.Deserializer.class)
public class ApiOperationImpl<S> extends AstNode<S> implements ApiOperation {

    private static final JavaType ERROR_LIST_TYPE =
            CollectionType.construct(
                    List.class,
                    null,
                    null,
                    null,
                    SimpleType.constructUnsafe(String.class)
            );

    private String name;

    private List<String> groups;

    private final List<ApiParameterImpl<S>> parameters = new ArrayList<>();

    private TypeRefImpl<S> returnType;

    private Map<TypeName, List<String>> errorMap = Collections.emptyMap();

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
            this.groups = Collections.emptyList();
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

    @Override
    public Map<TypeName, List<String>> getErrorMap() {
        return errorMap;
    }

    public void setErrorMap(Map<TypeName, List<String>> errorMap) {
        if (errorMap.isEmpty()) {
            this.errorMap = Collections.emptyMap();
        } else {
            Map<TypeName, List<String>> map = new LinkedHashMap<>((errorMap.size() * 4 + 2) / 3);
            for (Map.Entry<TypeName, List<String>> e : errorMap.entrySet()) {
                map.put(e.getKey(), Collections.unmodifiableList(e.getValue()));
            }
            this.errorMap = Collections.unmodifiableMap(map);
        }
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
            for (TypeName typeName : errorMap.keySet()) {
                TypeRefImpl<S> typeRef = new TypeRefImpl<>();
                typeRef.setTypeName(typeName);
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
            if (operation.getDoc() != null) {
                provider.defaultSerializeField("doc", operation.getDoc(), gen);
            }
            if (!operation.getParameters().isEmpty()) {
                provider.defaultSerializeField("parameters", operation.getParameters(), gen);
            }
            if (operation.getReturnType() != null) {
                provider.defaultSerializeField("returnType", operation.getReturnType(), gen);
            }
            if (!operation.getErrorMap().isEmpty()) {
                gen.writeFieldName("errors");
                gen.writeStartObject();
                for (Map.Entry<TypeName, List<String>> e : operation.getErrorMap().entrySet()) {
                    gen.writeFieldName(e.getKey().toString(true));
                    gen.writeObject(e.getValue());
                }
                gen.writeEndObject();
            }
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ApiOperationImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public ApiOperationImpl<?> deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JacksonException {
            JsonNode jsonNode = jp.getCodec().readTree(jp);
            String name = jsonNode.get("name").asText();
            ApiOperationImpl<Object> operation = new ApiOperationImpl<>(null, name);
            operation.setKey(jsonNode.get("key").asText());
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
            if (jsonNode.has("errors")) {
                Map<TypeName, List<String>> errorMap = new LinkedHashMap<>();
                for (Map.Entry<String, JsonNode> e : jsonNode.get("errors").properties()) {
                    errorMap.put(
                            TypeName.parse(e.getKey()),
                            ctx.readTreeAsValue(e.getValue(), ERROR_LIST_TYPE)
                    );
                }
                operation.setErrorMap(errorMap);
            }
            return operation;
        }
    }
}
