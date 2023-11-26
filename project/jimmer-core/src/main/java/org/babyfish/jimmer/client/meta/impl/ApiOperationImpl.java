package org.babyfish.jimmer.client.meta.impl;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.babyfish.jimmer.client.meta.ApiOperation;
import org.babyfish.jimmer.client.meta.ApiParameter;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.TypeRef;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonSerialize(using = ApiOperationImpl.Serializer.class)
@JsonDeserialize(using = ApiOperationImpl.Deserializer.class)
public class ApiOperationImpl<S> extends AstNode<S> implements ApiOperation {

    private String name;

    private List<String> groups;

    private List<ApiParameterImpl<S>> parameters;

    private TypeRefImpl<S> returnType;

    private Doc doc;

    ApiOperationImpl(S source, String name) {
        super(source);
        this.name = name;
        this.parameters = new ArrayList<>();
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
    }

    @Nullable
    @Override
    public TypeRef getReturnType() {
        return returnType;
    }

    public void setReturnType(TypeRefImpl<S> returnType) {
        this.returnType = returnType;
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
        for (ApiParameterImpl<S> parameter : parameters) {
            parameter.accept(visitor);
        }
        if (returnType != null) {
            returnType.accept(visitor);
        }
    }

    @Override
    public String toString() {
        return "ApiOperationImpl{" +
                "name='" + name + '\'' +
                ", parameters=" + parameters +
                ", returnType=" + returnType +
                ", doc='" + doc + '\'' +
                '}';
    }

    public static class Serializer extends JsonSerializer<ApiOperationImpl<?>> {

        @Override
        public void serialize(ApiOperationImpl<?> operation, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeFieldName("name");
            gen.writeString(operation.getName());
            if (operation.getDoc() != null) {
                provider.defaultSerializeField("doc", operation.getDoc(), gen);
            }
            if (!operation.getParameters().isEmpty()) {
                provider.defaultSerializeField("parameters", operation.getParameters(), gen);
            }
            if (operation.getReturnType() != null) {
                provider.defaultSerializeField("returnType", operation.getReturnType(), gen);
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
            return operation;
        }
    }
}
