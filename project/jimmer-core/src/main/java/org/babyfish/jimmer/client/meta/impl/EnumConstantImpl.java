package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.EnumConstant;

import java.io.IOException;

@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = EnumConstantImpl.SerializerV2.class)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = EnumConstantImpl.DeserializerV2.class)
@tools.jackson.databind.annotation.JsonSerialize(using = EnumConstantImpl.SerializerV3.class)
@tools.jackson.databind.annotation.JsonDeserialize(using = EnumConstantImpl.DeserializerV3.class)
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

    static class SerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<EnumConstantImpl<?>> {

        @Override
        public void serialize(EnumConstantImpl<?> constant,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            provider.defaultSerializeField("name", constant.getName(), gen);
            if (constant.getDoc() != null) {
                provider.defaultSerializeField("doc", constant.getDoc(), gen);
            }
            gen.writeEndObject();
        }
    }

    static class DeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<EnumConstantImpl<?>> {

        @Override
        public EnumConstantImpl<?> deserialize(com.fasterxml.jackson.core.JsonParser jp,
                                               com.fasterxml.jackson.databind.DeserializationContext ctx) throws IOException {
            com.fasterxml.jackson.databind.JsonNode jsonNode = jp.getCodec().readTree(jp);
            EnumConstantImpl<Object> constant = new EnumConstantImpl<>(null, jsonNode.get("name").asText());
            constant.setDoc(ctx.readTreeAsValue(jsonNode.get("doc"), Doc.class));
            return constant;
        }
    }

    static class SerializerV3 extends tools.jackson.databind.ValueSerializer<EnumConstantImpl<?>> {

        @Override
        public void serialize(EnumConstantImpl<?> constant,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeStartObject();
            ctx.defaultSerializeProperty("name", constant.getName(), gen);
            if (constant.getDoc() != null) {
                ctx.defaultSerializeProperty("doc", constant.getDoc(), gen);
            }
            gen.writeEndObject();
        }
    }

    static class DeserializerV3 extends tools.jackson.databind.ValueDeserializer<EnumConstantImpl<?>> {

        @Override
        public EnumConstantImpl<?> deserialize(tools.jackson.core.JsonParser jp,
                                               tools.jackson.databind.DeserializationContext ctx) {
            tools.jackson.databind.JsonNode jsonNode = ctx.readTree(jp);
            EnumConstantImpl<Object> constant = new EnumConstantImpl<>(null, jsonNode.get("name").asText());
            constant.setDoc(ctx.readTreeAsValue(jsonNode.get("doc"), Doc.class));
            return constant;
        }
    }
}