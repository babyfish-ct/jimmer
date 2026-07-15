package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.*;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ApiOperationImpl<S> extends AstNode<S> implements ApiOperation {

    private String name;

    private List<String> groups;

    private final List<ApiParameterImpl<S>> parameters = new ArrayList<>();

    private TypeRefImpl<S> returnType;

    private List<TypeRefImpl<S>> exceptionTypes = Collections.emptyList();

    private Doc doc;

    private StringBuilder keyBuilder;

    private String key;

    ApiOperationImpl(S source, String name) {
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
    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        if (groups == null || groups.isEmpty()) {
            this.groups = null;
        } else {
            this.groups = Collections.unmodifiableList(groups);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ApiParameter> getParameters() {
        return (List<ApiParameter>) (List<?>) parameters;
    }

    public void addParameter(ApiParameterImpl<S> parameter) {
        this.parameters.add(parameter);
        addIgnoredParameter(parameter);
    }

    public void addIgnoredParameter(ApiParameterImpl<S> parameter) {
        if (keyBuilder == null) {
            keyBuilder = new StringBuilder();
            keyBuilder.append(name);
        }
        TypeRef type = parameter.getType();
        if (type == null) {
            keyBuilder.append(':');
            return; // issue #887
        }
        TypeName typeName = parameter.getType().getTypeName();
        if (typeName.getTypeVariable() != null) {
            throw new AssertionError(
                    "Illegal parameter \"" + parameter.getName() + "\", its type cannot be type variable"
            );
        }
        if (typeName.isPrimitive() && parameter.getType().isNullable()) {
            typeName = typeName.box();
        }
        keyBuilder.append(':').append(typeName);
    }

    @Nullable
    @Override
    public TypeRef getReturnType() {
        return returnType;
    }

    public void setReturnType(TypeRefImpl<S> returnType) {
        this.returnType = returnType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<TypeRef> getExceptionTypes() {
        return (List<TypeRef>) (List<?>) exceptionTypes;
    }

    public void setExceptionTypeNames(Collection<TypeName> exceptionTypeNames) {
        if (exceptionTypeNames.isEmpty()) {
            this.exceptionTypes = Collections.emptyList();
        }
        List<TypeRefImpl<S>> typeRefs = new ArrayList<>(exceptionTypeNames.size());
        for (TypeName exceptionTypeName : exceptionTypeNames) {
            TypeRefImpl<S> typeRef = new TypeRefImpl<>();
            typeRef.setTypeName(exceptionTypeName);
            typeRefs.add(typeRef);
        }
        this.exceptionTypes = Collections.unmodifiableList(typeRefs);
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
            for (ApiParameterImpl<S> parameter : parameters) {
                parameter.accept(visitor);
            }
            if (returnType != null) {
                returnType.accept(visitor);
            }
            for (TypeRefImpl<S> typeRef : exceptionTypes) {
                typeRef.accept(visitor);
            }
        } finally {
            visitor.visitedAstNode(this);
        }
    }

    @Override
    public String toString() {
        return key().replaceFirst(":", "(").replace(":", ", ") + ')';
    }

    public String key() {
        String key = this.key;
        if (key == null) {
            if (keyBuilder == null) {
                this.key = key = name;
            } else {
                this.key = key = keyBuilder.toString();
                keyBuilder = null;
            }
        }
        return key;
    }

    public void setKey(String key) {
        this.key = key;
        this.keyBuilder = null;
    }

    }
