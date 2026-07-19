package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.Prop;
import org.babyfish.jimmer.client.meta.TypeRef;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Properties;

@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = PropImpl.SerializerV2.class)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = PropImpl.DeserializerV2.class)
@tools.jackson.databind.annotation.JsonSerialize(using = PropImpl.SerializerV3.class)
@tools.jackson.databind.annotation.JsonDeserialize(using = PropImpl.DeserializerV3.class)
public class PropImpl<S> extends AstNode<S> implements Prop {

    private String name;

    private Doc doc;

    private TypeRefImpl<S> type;

    PropImpl(S source, String name) {
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
    public TypeRef getType() {
        return type;
    }

    public void setType(TypeRefImpl<S> type) {
        this.type = type;
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
            type.accept(visitor);
        } finally {
            visitor.visitedAstNode(this);
        }
    }

    @Override
    public String toString() {
        return "PropImpl{" +
                "name='" + name + '\'' +
                ", doc=" + doc +
                ", type=" + type +
                '}';
    }

    void loadExportDoc(String declaringQualifiedName, Properties properties) {
        if (doc == null) {
            String docString = properties.getProperty(declaringQualifiedName + '.' + name);
            if (docString != null) {
                doc = Doc.parse(docString);
            }
        }
    }

    static class SerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<PropImpl<?>> {

        @Override
        public void serialize(PropImpl<?> prop,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeFieldName("name");
            gen.writeString(prop.getName());
            provider.defaultSerializeField("type", prop.getType(), gen);
            if (prop.getDoc() != null) {
                provider.defaultSerializeField("doc", prop.getDoc(), gen);
            }
            gen.writeEndObject();
        }
    }

    static class DeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<PropImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public PropImpl<?> deserialize(com.fasterxml.jackson.core.JsonParser jp,
                                       com.fasterxml.jackson.databind.DeserializationContext ctx) throws IOException {
            com.fasterxml.jackson.databind.JsonNode jsonNode = jp.getCodec().readTree(jp);
            PropImpl<Object> prop = new PropImpl<>(null, jsonNode.get("name").asText());
            prop.setType(ctx.readTreeAsValue(jsonNode.get("type"), TypeRefImpl.class));
            if (jsonNode.has("doc")) {
                prop.setDoc(ctx.readTreeAsValue(jsonNode.get("doc"), Doc.class));
            }
            return prop;
        }
    }

    static class SerializerV3 extends tools.jackson.databind.ValueSerializer<PropImpl<?>> {

        @Override
        public void serialize(PropImpl<?> prop,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeStartObject();
            gen.writeName("name");
            gen.writeString(prop.getName());
            ctx.defaultSerializeProperty("type", prop.getType(), gen);
            if (prop.getDoc() != null) {
                ctx.defaultSerializeProperty("doc", prop.getDoc(), gen);
            }
            gen.writeEndObject();
        }
    }

    static class DeserializerV3 extends tools.jackson.databind.ValueDeserializer<PropImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public PropImpl<?> deserialize(tools.jackson.core.JsonParser jp,
                                       tools.jackson.databind.DeserializationContext ctx) {
            tools.jackson.databind.JsonNode jsonNode = ctx.readTree(jp);
            PropImpl<Object> prop = new PropImpl<>(null, jsonNode.get("name").asText());
            prop.setType(ctx.readTreeAsValue(jsonNode.get("type"), TypeRefImpl.class));
            if (jsonNode.has("doc")) {
                prop.setDoc(ctx.readTreeAsValue(jsonNode.get("doc"), Doc.class));
            }
            return prop;
        }
    }
}
