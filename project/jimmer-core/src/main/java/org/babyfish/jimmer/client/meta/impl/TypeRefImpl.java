package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.meta.TypeRef;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    }
