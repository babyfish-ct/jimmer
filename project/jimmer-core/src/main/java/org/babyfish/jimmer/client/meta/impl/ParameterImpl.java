package org.babyfish.jimmer.client.meta.impl;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.babyfish.jimmer.client.meta.Parameter;
import org.babyfish.jimmer.client.meta.TypeRef;

import java.io.IOException;

@JsonSerialize(using = ParameterImpl.Serializer.class)
@JsonDeserialize(using = ParameterImpl.Deserializer.class)
public class ParameterImpl<S> extends AstNode<S> implements Parameter {

    private final String name;

    private TypeRefImpl<S> type;

    ParameterImpl(S source, String name) {
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
    public void accept(TypeNameVisitor visitor) {
        type.accept(visitor);
    }

    @Override
    public String toString() {
        return "ParameterImpl{" +
                "name='" + name + '\'' +
                ", type=" + type +
                '}';
    }

    public static class Serializer extends JsonSerializer<ParameterImpl<?>> {

        @Override
        public void serialize(ParameterImpl<?> parameter, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeFieldName("name");
            gen.writeString(parameter.getName());
            provider.defaultSerializeField("type", parameter.getType(), gen);
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ParameterImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public ParameterImpl<?> deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JacksonException {
            JsonNode jsonNode = jp.getCodec().readTree(jp);
            ParameterImpl<Object> parameter = new ParameterImpl<>(null, jsonNode.get("name").asText());
            parameter.setType((TypeRefImpl<Object>) ctx.readTreeAsValue(jsonNode.get("type"), TypeRefImpl.class));
            return parameter;
        }
    }
}
