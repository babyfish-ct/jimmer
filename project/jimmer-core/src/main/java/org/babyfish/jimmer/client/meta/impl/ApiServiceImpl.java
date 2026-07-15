package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.ApiIgnore;
import org.babyfish.jimmer.client.meta.ApiOperation;
import org.babyfish.jimmer.client.meta.ApiService;
import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.TypeName;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ApiServiceImpl<S> extends AstNode<S> implements ApiService {

    private TypeName typeName;

    private List<String> groups;

    private List<ApiOperationImpl<S>> operations = new ArrayList<>();

    private Doc doc;

    ApiServiceImpl(S source, TypeName typeName) {
        super(source);
        this.typeName = typeName;
    }

    @Override
    public TypeName getTypeName() {
        return typeName;
    }

    @Nullable
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
    public List<ApiOperation> getOperations() {
        return (List<ApiOperation>) (List<?>) operations;
    }

    @Nullable
    @Override
    public ApiOperation findOperation(String name, Parameter... parameters) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(name);
        for (Parameter parameter : parameters) {
            keyBuilder.append(':');
            if (parameter.isAnnotationPresent(ApiIgnore.class)) {
                continue;
            }
            Class<?> type = parameter.getType();
            if (type.isArray()) {
                keyBuilder.append(List.class.getName());
            } else if (Collection.class.isAssignableFrom(type)) {
                keyBuilder.append(List.class.getName());
            } else {
                keyBuilder.append(fullName(type));
            }
        }
        String key = keyBuilder.toString();
        for (ApiOperationImpl<S> operation : operations) {
            if (operation.key().equals(key)) {
                return operation;
            }
        }
        return null;
    }

    public void addOperation(ApiOperationImpl<S> operation) {
        this.operations.add(operation);
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
            for (ApiOperationImpl<S> operation : operations) {
                operation.accept(visitor);
            }
        } finally {
            visitor.visitedAstNode(this);
        }
    }

    @Override
    public String toString() {
        return typeName.toString();
    }

    public String value() {
        return "<api>";
    }

    private static String fullName(Class<?> type) {
        Class<?> declaringClass = type.getDeclaringClass();
        if (declaringClass != null) {
            return fullName(declaringClass) + '.' + type.getSimpleName();
        }
        return type.getName();
    }
}
