package org.babyfish.jimmer.client.meta.impl;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.babyfish.jimmer.client.meta.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JsonSerialize(using = TypeDefinitionImpl.Serializer.class)
@JsonDeserialize(using = TypeDefinitionImpl.Deserializer.class)
public class TypeDefinitionImpl<S> extends AstNode<S> implements TypeDefinition {

    private final TypeName typeName;

    private boolean immutable;

    private boolean apiIgnore;

    private Map<String, PropImpl<S>> propMap = new LinkedHashMap<>();

    private List<TypeRefImpl<S>> superTypes = new ArrayList<>();

    private Doc doc;

    TypeDefinitionImpl(S source, TypeName typeName) {
        super(source);
        this.typeName = typeName;
    }

    @Override
    public TypeName getTypeName() {
        return typeName;
    }

    @Override
    public boolean isImmutable() {
        return immutable;
    }

    public void setImmutable(boolean immutable) {
        this.immutable = immutable;
    }

    @Override
    public boolean isApiIgnore() {
        return apiIgnore;
    }

    public void setApiIgnore(boolean apiIgnore) {
        this.apiIgnore = apiIgnore;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Prop> getPropMap() {
        return (Map<String, Prop>) (Map<?, ?>)propMap;
    }

    public void addProp(PropImpl<S> prop) {
        propMap.put(prop.getName(), prop);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<TypeRef> getSuperTypes() {
        return (List<TypeRef>) (List<?>)superTypes;
    }

    public void addSuperType(TypeRefImpl<S> superType) {
        superTypes.add(superType);
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
            for (PropImpl<S> prop : propMap.values()) {
                prop.accept(visitor);
            }
            for (TypeRefImpl<S> superType : superTypes) {
                superType.accept(visitor);
            }
        } finally {
            visitor.visitedAstNode(this);
        }
    }

    @Override
    public String toString() {
        return "TypeDefinitionImpl{" +
                "typeName='" + typeName + '\'' +
                ", propMap=" + propMap +
                ", superTypes=" + superTypes +
                '}';
    }

    public static class Serializer extends JsonSerializer<TypeDefinitionImpl<?>> {

        @Override
        public void serialize(TypeDefinitionImpl<?> definition, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            provider.defaultSerializeField("typeName", definition.getTypeName(), gen);
            if (definition.isImmutable()) {
                gen.writeFieldName("immutable");
                gen.writeBoolean(true);
            }
            if (definition.isApiIgnore()) {
                gen.writeFieldName("apiIgnore");
                gen.writeBoolean(true);
            }
            if (!definition.getPropMap().isEmpty()) {
                provider.defaultSerializeField("props", definition.getPropMap().values(), gen);
            }
            if (!definition.getSuperTypes().isEmpty()) {
                provider.defaultSerializeField("superTypes", definition.getSuperTypes(), gen);
            }
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<TypeDefinitionImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public TypeDefinitionImpl<?> deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JacksonException {
            JsonNode jsonNode = jp.getCodec().readTree(jp);
            TypeDefinitionImpl<Object> definition = new TypeDefinitionImpl<>(
                    null,
                    ctx.readTreeAsValue(jsonNode.get("typeName"), TypeName.class)
            );
            if (jsonNode.has("immutable")) {
                definition.setImmutable(jsonNode.get("immutable").asBoolean());
            }
            if (jsonNode.has("apiIgnore")) {
                definition.setApiIgnore(jsonNode.get("apiIgnore").asBoolean());
            }
            if (jsonNode.has("props")) {
                for (JsonNode propNode : jsonNode.get("props")) {
                    definition.addProp(ctx.readTreeAsValue(propNode, PropImpl.class));
                }
            }
            if (jsonNode.has("superTypes")) {
                for (JsonNode superNode : jsonNode.get("superTypes")) {
                    definition.addSuperType(ctx.readTreeAsValue(superNode, TypeRefImpl.class));
                }
            }
            return definition;
        }
    }
}
