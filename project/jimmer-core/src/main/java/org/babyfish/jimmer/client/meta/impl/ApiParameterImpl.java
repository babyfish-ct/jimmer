package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.ApiParameter;
import org.babyfish.jimmer.client.meta.TypeRef;

import java.io.IOException;

@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = ApiParameterImpl.SerializerV2.class)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = ApiParameterImpl.DeserializerV2.class)
@tools.jackson.databind.annotation.JsonSerialize(using = ApiParameterImpl.SerializerV3.class)
@tools.jackson.databind.annotation.JsonDeserialize(using = ApiParameterImpl.DeserializerV3.class)
public class ApiParameterImpl<S> extends AstNode<S> implements ApiParameter {

    private final String name;

    private TypeRefImpl<S> type;

    private int originalIndex;

    ApiParameterImpl(S source, String name) {
        super(source);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public TypeRef getType() {
        return type;
    }

    public void setType(TypeRefImpl<S> type) {
        this.type = type;
    }

    @Override
    public int getOriginalIndex() {
        return originalIndex;
    }

    public void setOriginalIndex(int originalIndex) {
        this.originalIndex = originalIndex;
    }

    @Override
    public void accept(AstNodeVisitor<S> visitor) {
        try {
            if (!visitor.visitAstNode(this)) {
                return;
            }
            type.accept(visitor);
        } finally {
            visitor.visitedAstNode(this);
        }
    }

    @Override
    public String toString() {
        return "ParameterImpl{" +
                "name='" + name + '\'' +
                ", type=" + type +
                '}';
    }

    static class SerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<ApiParameterImpl<?>> {

        @Override
        public void serialize(ApiParameterImpl<?> parameter,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeFieldName("name");
            gen.writeString(parameter.getName());
            provider.defaultSerializeField("type", parameter.getType(), gen);
            gen.writeFieldName("index");
            gen.writeNumber(parameter.getOriginalIndex());
            gen.writeEndObject();
        }
    }

    static class DeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<ApiParameterImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public ApiParameterImpl<?> deserialize(com.fasterxml.jackson.core.JsonParser jp,
                                               com.fasterxml.jackson.databind.DeserializationContext ctx) throws IOException {
            com.fasterxml.jackson.databind.JsonNode jsonNode = jp.getCodec().readTree(jp);
            ApiParameterImpl<Object> parameter = new ApiParameterImpl<>(null, jsonNode.get("name").asText());
            parameter.setType((TypeRefImpl<Object>) ctx.readTreeAsValue(jsonNode.get("type"), TypeRefImpl.class));
            parameter.setOriginalIndex(jsonNode.get("index").asInt());
            return parameter;
        }
    }

    static class SerializerV3 extends tools.jackson.databind.ValueSerializer<ApiParameterImpl<?>> {

        @Override
        public void serialize(ApiParameterImpl<?> parameter,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeStartObject();
            gen.writeName("name");
            gen.writeString(parameter.getName());
            ctx.defaultSerializeProperty("type", parameter.getType(), gen);
            gen.writeName("index");
            gen.writeNumber(parameter.getOriginalIndex());
            gen.writeEndObject();
        }
    }

    static class DeserializerV3 extends tools.jackson.databind.ValueDeserializer<ApiParameterImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public ApiParameterImpl<?> deserialize(tools.jackson.core.JsonParser jp,
                                               tools.jackson.databind.DeserializationContext ctx) {
            tools.jackson.databind.JsonNode jsonNode = ctx.readTree(jp);
            ApiParameterImpl<Object> parameter = new ApiParameterImpl<>(null, jsonNode.get("name").asText());
            parameter.setType((TypeRefImpl<Object>) ctx.readTreeAsValue(jsonNode.get("type"), TypeRefImpl.class));
            parameter.setOriginalIndex(jsonNode.get("index").asInt());
            return parameter;
        }
    }
}