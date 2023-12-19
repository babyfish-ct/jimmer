package org.babyfish.jimmer.client.meta.impl;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.EnumConstant;

import java.io.IOException;

@JsonSerialize(using = EnumConstantImpl.Serializer.class)
@JsonDeserialize(using = EnumConstantImpl.Deserializer.class)
public class EnumConstantImpl<S> extends AstNode<S> implements EnumConstant {

    private final String name;

    private Doc doc;

    public EnumConstantImpl(S source, String name) {
        super(source);
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

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
        visitor.visitedAstNode(this);
    }

    public static class Serializer extends JsonSerializer<EnumConstantImpl<?>> {

        @Override
        public void serialize(EnumConstantImpl<?> constant, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            provider.defaultSerializeField("name", constant.getName(), gen);
            if (constant.getDoc() != null) {
                provider.defaultSerializeField("doc", constant.getDoc(), gen);
            }
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<EnumConstantImpl<?>> {

        @Override
        public EnumConstantImpl<?> deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JacksonException {
            JsonNode jsonNode = jp.getCodec().readTree(jp);
            EnumConstantImpl<Object> constant = new EnumConstantImpl<>(null, jsonNode.get("name").asText());
            constant.setDoc(ctx.readTreeAsValue(jsonNode.get("doc"), Doc.class));
            return constant;
        }
    }
}
