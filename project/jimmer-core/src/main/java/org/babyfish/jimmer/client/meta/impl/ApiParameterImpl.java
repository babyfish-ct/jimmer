package org.babyfish.jimmer.client.meta.impl;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.babyfish.jimmer.client.meta.ApiParameter;
import org.babyfish.jimmer.client.meta.TypeRef;

import java.io.IOException;

@JsonSerialize(using = ApiParameterImpl.Serializer.class)
@JsonDeserialize(using = ApiParameterImpl.Deserializer.class)
public class ApiParameterImpl<S> extends AstNode<S> implements ApiParameter {

    private final String name;

    private TypeRefImpl<S> type;

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
    public void accept(AstNodeVisitor<S> visitor) {
        visitor.visitAstNode(this);
        type.accept(visitor);
    }

    @Override
    public String toString() {
        return "ParameterImpl{" +
                "name='" + name + '\'' +
                ", type=" + type +
                '}';
    }

    public static class Serializer extends JsonSerializer<ApiParameterImpl<?>> {

        @Override
        public void serialize(ApiParameterImpl<?> parameter, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeFieldName("name");
            gen.writeString(parameter.getName());
            provider.defaultSerializeField("type", parameter.getType(), gen);
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ApiParameterImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public ApiParameterImpl<?> deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JacksonException {
            JsonNode jsonNode = jp.getCodec().readTree(jp);
            ApiParameterImpl<Object> parameter = new ApiParameterImpl<>(null, jsonNode.get("name").asText());
            parameter.setType((TypeRefImpl<Object>) ctx.readTreeAsValue(jsonNode.get("type"), TypeRefImpl.class));
            return parameter;
        }
    }
}
