package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@com.fasterxml.jackson.databind.annotation.JsonSerialize(using = TypeDefinitionImpl.SerializerV2.class)
@com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = TypeDefinitionImpl.DeserializerV2.class)
@tools.jackson.databind.annotation.JsonSerialize(using = TypeDefinitionImpl.SerializerV3.class)
@tools.jackson.databind.annotation.JsonDeserialize(using = TypeDefinitionImpl.DeserializerV3.class)
public class TypeDefinitionImpl<S> extends AstNode<S> implements TypeDefinition {

    private final TypeName typeName;

    private Kind kind;

    private Error error;

    private boolean apiIgnore;

    private List<String> groups = Collections.emptyList();

    private final Map<String, PropImpl<S>> propMap = new LinkedHashMap<>();

    private final List<TypeRefImpl<S>> superTypes = new ArrayList<>();

    private final List<TypeRefImpl<S>> polymorphicBranches = new ArrayList<>();

    private Doc doc;

    private final Map<String, EnumConstantImpl<S>> enumConstantMap = new LinkedHashMap<>();

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
        return (List<TypeRef>) (List<?>) superTypes;
    }

    public void addSuperType(TypeRefImpl<S> superType) {
        superTypes.add(superType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<TypeRef> getPolymorphicBranches() {
        return (List<TypeRef>) (List<?>) polymorphicBranches;
    }

    public void addPolymorphicBranch(TypeRefImpl<S> branch) {
        polymorphicBranches.add(branch);
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
            for (TypeRefImpl<S> branch : polymorphicBranches) {
                branch.accept(visitor);
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
                ", polymorphicBranches=" + polymorphicBranches +
                '}';
    }

    public void loadExportDoc(Properties properties) {
        StringBuilder builder = new StringBuilder();
        boolean addDot = false;
        if (typeName.getPackageName() != null) {
            builder.append(typeName.getPackageName());
            addDot = true;
        }
        for (String simpleName : getTypeName().getSimpleNames()) {
            if (addDot) {
                builder.append('.');
            } else {
                addDot = true;
            }
            builder.append(simpleName);
        }
        String qualifiedName = builder.toString();
        if (doc == null) {
            String docString = properties.getProperty(qualifiedName);
            if (docString != null) {
                doc = Doc.parse(docString);
            }
        }
        for (PropImpl<?> prop : propMap.values()) {
            prop.loadExportDoc(qualifiedName, properties);
        }
    }

    static class SerializerV2 extends com.fasterxml.jackson.databind.JsonSerializer<TypeDefinitionImpl<?>> {

        @Override
        public void serialize(TypeDefinitionImpl<?> definition,
                              com.fasterxml.jackson.core.JsonGenerator gen,
                              com.fasterxml.jackson.databind.SerializerProvider provider) throws IOException {
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
            if (!definition.getPolymorphicBranches().isEmpty()) {
                provider.defaultSerializeField("branches", definition.getPolymorphicBranches(), gen);
            }
            if (!definition.getEnumConstantMap().isEmpty()) {
                provider.defaultSerializeField("constants", definition.getEnumConstantMap().values(), gen);
            }
            gen.writeEndObject();
        }
    }

    static class DeserializerV2 extends com.fasterxml.jackson.databind.JsonDeserializer<TypeDefinitionImpl<?>> {

        private static final com.fasterxml.jackson.databind.JavaType GROUPS_TYPE =
                com.fasterxml.jackson.databind.type.CollectionType.construct(
                        List.class,
                        null,
                        null,
                        null,
                        com.fasterxml.jackson.databind.type.SimpleType.constructUnsafe(String.class)
                );

        @SuppressWarnings("unchecked")
        @Override
        public TypeDefinitionImpl<?> deserialize(com.fasterxml.jackson.core.JsonParser jp,
                                                 com.fasterxml.jackson.databind.DeserializationContext ctx) throws IOException {
            com.fasterxml.jackson.databind.JsonNode jsonNode = jp.getCodec().readTree(jp);
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
            if (jsonNode.has("apiIgnore")) {
                definition.setApiIgnore(jsonNode.get("apiIgnore").asBoolean());
            }
            if (jsonNode.has("props")) {
                for (com.fasterxml.jackson.databind.JsonNode propNode : jsonNode.get("props")) {
                    definition.addProp(ctx.readTreeAsValue(propNode, PropImpl.class));
                }
            }
            if (jsonNode.has("superTypes")) {
                for (com.fasterxml.jackson.databind.JsonNode superNode : jsonNode.get("superTypes")) {
                    definition.addSuperType(ctx.readTreeAsValue(superNode, TypeRefImpl.class));
                }
            }
            if (jsonNode.has("branches")) {
                for (com.fasterxml.jackson.databind.JsonNode branchNode : jsonNode.get("branches")) {
                    definition.addPolymorphicBranch(ctx.readTreeAsValue(branchNode, TypeRefImpl.class));
                }
            }
            if (jsonNode.has("constants")) {
                for (com.fasterxml.jackson.databind.JsonNode propNode : jsonNode.get("constants")) {
                    definition.addEnumConstant(ctx.readTreeAsValue(propNode, EnumConstantImpl.class));
                }
            }
            return definition;
        }
    }

    static class SerializerV3 extends tools.jackson.databind.ValueSerializer<TypeDefinitionImpl<?>> {

        @Override
        public void serialize(TypeDefinitionImpl<?> definition,
                              tools.jackson.core.JsonGenerator gen,
                              tools.jackson.databind.SerializationContext ctx) {
            gen.writeStartObject();
            ctx.defaultSerializeProperty("typeName", definition.getTypeName(), gen);
            if (definition.getKind() != Kind.OBJECT) {
                gen.writeName("kind");
                gen.writeString(definition.getKind().name());
            }
            if (definition.getDoc() != null) {
                ctx.defaultSerializeProperty("doc", definition.getDoc(), gen);
            }
            if (definition.getError() != null) {
                ctx.defaultSerializeProperty("error", definition.getError(), gen);
            }
            if (definition.isApiIgnore()) {
                gen.writeName("apiIgnore");
                gen.writeBoolean(true);
            }
            if (definition.getGroups() != null) {
                ctx.defaultSerializeProperty("groups", definition.getGroups(), gen);
            }
            if (!definition.getPropMap().isEmpty()) {
                ctx.defaultSerializeProperty("props", definition.getPropMap().values(), gen);
            }
            if (!definition.getSuperTypes().isEmpty()) {
                ctx.defaultSerializeProperty("superTypes", definition.getSuperTypes(), gen);
            }
            if (!definition.getPolymorphicBranches().isEmpty()) {
                ctx.defaultSerializeProperty("branches", definition.getPolymorphicBranches(), gen);
            }
            if (!definition.getEnumConstantMap().isEmpty()) {
                ctx.defaultSerializeProperty("constants", definition.getEnumConstantMap().values(), gen);
            }
            gen.writeEndObject();
        }
    }

    static class DeserializerV3 extends tools.jackson.databind.ValueDeserializer<TypeDefinitionImpl<?>> {

        @SuppressWarnings("unchecked")
        @Override
        public TypeDefinitionImpl<?> deserialize(tools.jackson.core.JsonParser jp,
                                                 tools.jackson.databind.DeserializationContext ctx) {
            tools.jackson.databind.JsonNode jsonNode = ctx.readTree(jp);
            TypeDefinitionImpl<Object> definition = new TypeDefinitionImpl<>(
                    null,
                    ctx.readTreeAsValue(jsonNode.get("typeName"), TypeName.class)
            );
            if (jsonNode.has("groups")) {
                definition.mergeGroups(
                        Collections.unmodifiableList(
                                ctx.readTreeAsValue(jsonNode.get("groups"),
                                        ctx.getTypeFactory().constructCollectionType(List.class, String.class))
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
            if (jsonNode.has("apiIgnore")) {
                definition.setApiIgnore(jsonNode.get("apiIgnore").asBoolean());
            }
            if (jsonNode.has("props")) {
                for (tools.jackson.databind.JsonNode propNode : jsonNode.get("props")) {
                    definition.addProp(ctx.readTreeAsValue(propNode, PropImpl.class));
                }
            }
            if (jsonNode.has("superTypes")) {
                for (tools.jackson.databind.JsonNode superNode : jsonNode.get("superTypes")) {
                    definition.addSuperType(ctx.readTreeAsValue(superNode, TypeRefImpl.class));
                }
            }
            if (jsonNode.has("branches")) {
                for (tools.jackson.databind.JsonNode branchNode : jsonNode.get("branches")) {
                    definition.addPolymorphicBranch(ctx.readTreeAsValue(branchNode, TypeRefImpl.class));
                }
            }
            if (jsonNode.has("constants")) {
                for (tools.jackson.databind.JsonNode propNode : jsonNode.get("constants")) {
                    definition.addEnumConstant(ctx.readTreeAsValue(propNode, EnumConstantImpl.class));
                }
            }
            return definition;
        }
    }
}
