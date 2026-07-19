package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.meta.TypeRef;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = TypeRefImpl.SerializerV2.class)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = TypeRefImpl.DeserializerV2.class)
@tools.jackson.databind.annotation.JsonSerialize(using = TypeRefImpl.SerializerV3.class)
@tools.jackson.databind.annotation.JsonDeserialize(using = TypeRefImpl.DeserializerV3.class)
public class TypeRefImpl<S> extends AstNode<S> implements TypeRef {

    private TypeName typeName;

    private boolean nullable;

    private List<TypeRefImpl<S>> arguments = new ArrayList<>();

    private String fetchBy;

    private TypeName fetchOwner;

    private Doc fetcherDoc;

    public TypeRefImpl() {
        super(null);
    }

    @Override
    public TypeName getTypeName() {
        return typeName;
    }

    public void setTypeName(TypeName typeName) {
        this.typeName = typeName;
    }

    @Override
    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<TypeRef> getArguments() {
        return (List<TypeRef>) (List<?>) arguments;
    }

    public void addArgument(TypeRefImpl<S> argument) {
        this.arguments.add(argument);
    }

    @Nullable
    @Override
    public String getFetchBy() {
        return fetchBy;
    }

    public void setFetchBy(String fetchBy) {
        this.fetchBy = fetchBy;
    }

    @Nullable
    @Override
    public TypeName getFetcherOwner() {
        return fetchOwner;
    }

    public void setFetcherOwner(TypeName fetchOwner) {
        this.fetchOwner = fetchOwner;
    }

    @Nullable
    @Override
    public Doc getFetcherDoc() {
        return fetcherDoc;
    }

    public void setFetcherDoc(Doc fetcherDoc) {
        this.fetcherDoc = fetcherDoc;
    }

    public void replaceBy(TypeRefImpl<S> typeRef, Boolean isNullable) {
        source = typeRef.source;
        typeName = typeRef.typeName;
        nullable = isNullable != null ? isNullable : typeRef.nullable;
        fetchBy = typeRef.fetchBy;
        fetchOwner = typeRef.fetchOwner;
        fetcherDoc = typeRef.fetcherDoc;
    }

    @Override
    public void accept(AstNodeVisitor<S> visitor) {
        try {
            if (!visitor.visitAstNode(this)) {
                return;
            }
            if (arguments != null) {
                for (TypeRefImpl<S> argument : arguments) {
                    argument.accept(visitor);
                }
            }
        } finally {
            visitor.visitedAstNode(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeRefImpl<?> typeRef = (TypeRefImpl<?>) o;

        if (nullable != typeRef.nullable) return false;
        if (!typeName.equals(typeRef.typeName)) return false;
        if (!arguments.equals(typeRef.arguments)) return false;
        if (!Objects.equals(fetchBy, typeRef.fetchBy)) return false;
        return Objects.equals(fetchOwner, typeRef.fetchOwner);
    }

    @Override
    public int hashCode() {
        int result = typeName.hashCode();
        result = 31 * result + (nullable ? 1 : 0);
        result = 31 * result + arguments.hashCode();
        result = 31 * result + (fetchBy != null ? fetchBy.hashCode() : 0);
        result = 31 * result + (fetchOwner != null ? fetchOwner.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "TypeRefImpl{" +
                "typeName='" + typeName + '\'' +
                ", nullable=" + nullable +
                ", arguments=" + arguments +
                ", fetchBy='" + fetchBy + '\'' +
                ", fetchOwner='" + fetchOwner + '\'' +
                '}';
    }

    static class SerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<TypeRefImpl<?>> {

        @Override
        public void serialize(TypeRefImpl<?> typeRef,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            provider.defaultSerializeField("typeName", typeRef.getTypeName(), gen);
            if (typeRef.isNullable()) {
                gen.writeFieldName("nullable");
                gen.writeBoolean(true);
            }
            if (!typeRef.getArguments().isEmpty()) {
                provider.defaultSerializeField("arguments", typeRef.getArguments(), gen);
            }
            if (typeRef.getFetchBy() != null) {
                gen.writeFieldName("fetchBy");
                gen.writeString(typeRef.getFetchBy());
                gen.writeFieldName("fetcherOwner");
                gen.writeString(typeRef.getFetcherOwner().toString());
                provider.defaultSerializeField("fetcherDoc", typeRef.getFetcherDoc(), gen);
            }
            gen.writeEndObject();
        }
    }

    static class DeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<TypeRefImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public TypeRefImpl<?> deserialize(com.fasterxml.jackson.core.JsonParser jp,
                                          com.fasterxml.jackson.databind.DeserializationContext ctx) throws IOException {
            com.fasterxml.jackson.databind.JsonNode jsonNode = jp.getCodec().readTree(jp);
            TypeRefImpl<Object> typeRef = new TypeRefImpl<>();
            typeRef.setTypeName(ctx.readTreeAsValue(jsonNode.get("typeName"), TypeName.class));
            if (jsonNode.has("nullable")) {
                typeRef.setNullable(jsonNode.get("nullable").asBoolean());
            }
            if (jsonNode.has("arguments")) {
                for (com.fasterxml.jackson.databind.JsonNode argNode : jsonNode.get("arguments")) {
                    TypeRefImpl<Object> arg = (TypeRefImpl<Object>) ctx.readTreeAsValue(argNode, TypeRefImpl.class);
                    typeRef.addArgument(arg);
                }
            }
            if (jsonNode.has("fetchBy")) {
                typeRef.setFetchBy(jsonNode.get("fetchBy").asText());
                typeRef.setFetcherOwner(TypeName.parse(jsonNode.get("fetcherOwner").asText()));
                if (jsonNode.has("fetcherDoc")) {
                    typeRef.setFetcherDoc(ctx.readTreeAsValue(jsonNode.get("fetcherDoc"), Doc.class));
                }
            }
            return typeRef;
        }
    }

    static class SerializerV3 extends tools.jackson.databind.ValueSerializer<TypeRefImpl<?>> {

        @Override
        public void serialize(TypeRefImpl<?> typeRef,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeStartObject();
            ctx.defaultSerializeProperty("typeName", typeRef.getTypeName(), gen);
            if (typeRef.isNullable()) {
                gen.writeName("nullable");
                gen.writeBoolean(true);
            }
            if (!typeRef.getArguments().isEmpty()) {
                ctx.defaultSerializeProperty("arguments", typeRef.getArguments(), gen);
            }
            if (typeRef.getFetchBy() != null) {
                gen.writeName("fetchBy");
                gen.writeString(typeRef.getFetchBy());
                gen.writeName("fetcherOwner");
                gen.writeString(typeRef.getFetcherOwner().toString());
                ctx.defaultSerializeProperty("fetcherDoc", typeRef.getFetcherDoc(), gen);
            }
            gen.writeEndObject();
        }
    }

    static class DeserializerV3 extends tools.jackson.databind.ValueDeserializer<TypeRefImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public TypeRefImpl<?> deserialize(tools.jackson.core.JsonParser jp,
                                          tools.jackson.databind.DeserializationContext ctx) {
            tools.jackson.databind.JsonNode jsonNode = ctx.readTree(jp);
            TypeRefImpl<Object> typeRef = new TypeRefImpl<>();
            typeRef.setTypeName(ctx.readTreeAsValue(jsonNode.get("typeName"), TypeName.class));
            if (jsonNode.has("nullable")) {
                typeRef.setNullable(jsonNode.get("nullable").asBoolean());
            }
            if (jsonNode.has("arguments")) {
                for (tools.jackson.databind.JsonNode argNode : jsonNode.get("arguments")) {
                    TypeRefImpl<Object> arg = (TypeRefImpl<Object>) ctx.readTreeAsValue(argNode, TypeRefImpl.class);
                    typeRef.addArgument(arg);
                }
            }
            if (jsonNode.has("fetchBy")) {
                typeRef.setFetchBy(jsonNode.get("fetchBy").asText());
                typeRef.setFetcherOwner(TypeName.parse(jsonNode.get("fetcherOwner").asText()));
                if (jsonNode.has("fetcherDoc")) {
                    typeRef.setFetcherDoc(ctx.readTreeAsValue(jsonNode.get("fetcherDoc"), Doc.class));
                }
            }
            return typeRef;
        }
    }
}
