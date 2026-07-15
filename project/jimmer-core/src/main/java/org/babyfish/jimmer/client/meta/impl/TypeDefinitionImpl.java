package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

    }
