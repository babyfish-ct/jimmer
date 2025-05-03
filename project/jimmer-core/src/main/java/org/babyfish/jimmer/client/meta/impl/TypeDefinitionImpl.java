package org.babyfish.jimmer.client.meta.impl;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.SimpleType;
import org.babyfish.jimmer.client.meta.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@JsonSerialize(using = TypeDefinitionImpl.Serializer.class)
@JsonDeserialize(using = TypeDefinitionImpl.Deserializer.class)
public class TypeDefinitionImpl<S> extends AstNode<S> implements TypeDefinition {

    private final TypeName typeName;

    private Kind kind;

    private Error error;

    private boolean apiIgnore;

    private List<String> groups = Collections.emptyList();

    private final Map<String, PropImpl<S>> propMap = new LinkedHashMap<>();

    private final List<TypeRefImpl<S>> superTypes = new ArrayList<>();

    private Doc doc;

    private final Map<String, EnumConstantImpl<S>> enumConstantMap = new LinkedHashMap<>();

    private boolean isRealModuleType = false;

    public boolean isRealModuleType() {
        return isRealModuleType;
    }

    public void setRealModuleType(boolean realModuleType) {
        isRealModuleType = realModuleType;
    }

    TypeDefinitionImpl(S source, TypeName typeName) {
        super(source);
        this.typeName = typeName;
    }

    @Override
    public TypeName getTypeName() {
        return typeName;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    @Nullable
    @Override
    public Doc getDoc() {
        return doc;
    }

    public void setDoc(Doc doc) {
        this.doc = doc;
    }

    @Nullable
    @Override
    public Error getError() {
        return error;
    }

    public void setError(Error error) {
        this.error = error;
    }

    @Override
    public boolean isApiIgnore() {
        return apiIgnore;
    }

    @Nullable
    @Override
    public List<String> getGroups() {
        List<String> l = groups;
        return l == null || l.isEmpty() ? null : l;
    }

    public void mergeGroups(List<String> groups) {
        if (this.groups == null) {
            return;
        }
        if (groups == null || groups.isEmpty()) {
            this.groups = null;
            return;
        }
        List<String> merged = new ArrayList<>(this.groups);
        merged.addAll(groups);
        this.groups = merged.stream().distinct().collect(Collectors.toList());
    }

    public void setApiIgnore(boolean apiIgnore) {
        this.apiIgnore = apiIgnore;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Prop> getPropMap() {
        return (Map<String, Prop>) (Map<?, ?>) propMap;
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

    @SuppressWarnings("unchecked")
    @Override
    public Map<String, EnumConstant> getEnumConstantMap() {
        return (Map<String, EnumConstant>) (Map<?, ?>) enumConstantMap;
    }

    public void addEnumConstant(EnumConstantImpl<S> constant) {
        enumConstantMap.put(constant.getName(), constant);
    }

    @Override
    public void accept(AstNodeVisitor<S> visitor) {
        try {
            if (!visitor.visitAstNode(this)) {
                return;
            }
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
            if (definition.getKind() != Kind.OBJECT) {
                gen.writeFieldName("kind");
                gen.writeString(definition.getKind().name());
            }
            if (definition.getDoc() != null) {
                provider.defaultSerializeField("doc", definition.getDoc(), gen);
            }
            if (definition.getError() != null) {
                provider.defaultSerializeField("error", definition.getError(), gen);
            }
            if (definition.isRealModuleType()) {
                provider.defaultSerializeField("isRealModuleType", definition.isRealModuleType(), gen);
            }
            if (definition.isApiIgnore()) {
                gen.writeFieldName("apiIgnore");
                gen.writeBoolean(true);
            }
            if (definition.getGroups() != null) {
                provider.defaultSerializeField("groups", definition.getGroups(), gen);
            }
            if (!definition.getPropMap().isEmpty()) {
                provider.defaultSerializeField("props", definition.getPropMap().values(), gen);
            }
            if (!definition.getSuperTypes().isEmpty()) {
                provider.defaultSerializeField("superTypes", definition.getSuperTypes(), gen);
            }
            if (!definition.getEnumConstantMap().isEmpty()) {
                provider.defaultSerializeField("constants", definition.getEnumConstantMap().values(), gen);
            }
            gen.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<TypeDefinitionImpl<?>> {

        private static final JavaType GROUPS_TYPE = CollectionType.construct(
                List.class,
                null,
                null,
                null,
                SimpleType.constructUnsafe(String.class)
        );

        @SuppressWarnings("unchecked")
        @Override
        public TypeDefinitionImpl<?> deserialize(JsonParser jp, DeserializationContext ctx) throws IOException, JacksonException {
            JsonNode jsonNode = jp.getCodec().readTree(jp);
            TypeDefinitionImpl<Object> definition = new TypeDefinitionImpl<>(
                    null,
                    ctx.readTreeAsValue(jsonNode.get("typeName"), TypeName.class)
            );
            if (jsonNode.has("groups")) {
                definition.mergeGroups(
                        Collections.unmodifiableList(
                                ctx.readTreeAsValue(jsonNode.get("groups"), GROUPS_TYPE)
                        )
                );
                if (!Schemas.isAllowed(ctx, definition.getGroups())) {
                    return definition;
                }
            } else {
                definition.mergeGroups(null);
            }
            if (jsonNode.has("kind")) {
                definition.setKind(Kind.valueOf(jsonNode.get("kind").asText()));
            }
            if (jsonNode.has("doc")) {
                definition.setDoc(ctx.readTreeAsValue(jsonNode.get("doc"), Doc.class));
            }
            if (jsonNode.has("error")) {
                definition.setError(ctx.readTreeAsValue(jsonNode.get("error"), Error.class));
            }
            if (jsonNode.has("isRealModuleType")) {
                definition.setRealModuleType(jsonNode.get("isRealModuleType").asBoolean());
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
            if (jsonNode.has("constants")) {
                for (JsonNode propNode : jsonNode.get("constants")) {
                    definition.addEnumConstant(ctx.readTreeAsValue(propNode, EnumConstantImpl.class));
                }
            }
            return definition;
        }
    }
}
