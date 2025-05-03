package org.babyfish.jimmer.client.meta.impl;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.Prop;
import org.babyfish.jimmer.client.meta.TypeRef;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@JsonSerialize(using = PropImpl.Serializer.class)
@JsonDeserialize(using = PropImpl.Deserializer.class)
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

    public void accept(AstNodeVisitor<S> visitor,boolean onlySaveCurrentModuleClass) {
        try {
            if (!visitor.visitAstNode(this,onlySaveCurrentModuleClass)) {
                return;
            }
            type.accept(visitor,onlySaveCurrentModuleClass);
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

    public static class Serializer extends JsonSerializer<PropImpl<?>> {

        @Override
        public void serialize(PropImpl<?> prop, JsonGenerator gen, SerializerProvider provider) throws IOException {
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

    public static class Deserializer extends JsonDeserializer<PropImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public PropImpl<?> deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JacksonException {
            JsonNode jsonNode = jp.getCodec().readTree(jp);
            PropImpl<Object> prop = new PropImpl<>(null, jsonNode.get("name").asText());
            prop.setType(ctx.readTreeAsValue(jsonNode.get("type"), TypeRefImpl.class));
            if (jsonNode.has("doc")) {
                prop.setDoc(ctx.readTreeAsValue(jsonNode.get("doc"), Doc.class));
            }
            return prop;
        }
    }
}
