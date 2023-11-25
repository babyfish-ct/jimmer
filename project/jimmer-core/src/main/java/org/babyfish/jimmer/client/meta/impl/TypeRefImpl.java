package org.babyfish.jimmer.client.meta.impl;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.babyfish.jimmer.client.meta.TypeRef;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonSerialize(using = TypeRefImpl.Serializer.class)
@JsonDeserialize(using = TypeRefImpl.Deserializer.class)
public class TypeRefImpl<S> extends AstNode<S> implements TypeRef {

    private String typeName;

    private boolean nullable;

    private List<TypeRefImpl<S>> arguments = new ArrayList<>();

    private String fetchBy;

    private String fetchOwner;

    TypeRefImpl() {
        super(null);
    }

    @Override
    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
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
        return (List<TypeRef>) (List<?>)arguments;
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
    public String getFetcherOwner() {
        return fetchOwner;
    }

    public void setFetchOwner(String fetchOwner) {
        this.fetchOwner = fetchOwner;
    }

    @Override
    public void accept(TypeNameVisitor visitor) {
        visitor.visitTypeName(typeName);
        if (arguments != null) {
            for (TypeRefImpl<S> argument : arguments) {
                argument.accept(visitor);
            }
        }
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

    public static class Serializer extends JsonSerializer<TypeRefImpl<?>> {

        @Override
        public void serialize(TypeRefImpl<?> typeRef, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeStartObject();
            gen.writeFieldName("typeName");
            gen.writeString(typeRef.getTypeName());
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
                gen.writeString(typeRef.getFetcherOwner());
            }
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<TypeRefImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public TypeRefImpl<?> deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JacksonException {
            JsonNode jsonNode = jp.getCodec().readTree(jp);
            TypeRefImpl<Object> typeRef = new TypeRefImpl<>();
            typeRef.setTypeName(jsonNode.get("typeName").asText());
            if (jsonNode.has("nullable")) {
                typeRef.setNullable(jsonNode.get("nullable").asBoolean());
            }
            if (jsonNode.has("arguments")) {
                for (JsonNode argNode : jsonNode.get("arguments")) {
                    TypeRefImpl<Object> arg = (TypeRefImpl<Object>) ctx.readTreeAsValue(argNode, TypeRefImpl.class);
                    typeRef.addArgument(arg);
                }
            }
            if (jsonNode.has("fetchBy")) {
                typeRef.setFetchBy(jsonNode.get("fetchBy").asText());
                typeRef.setFetchOwner(jsonNode.get("fetcherOwner").asText());
            }
            return typeRef;
        }
    }
}
