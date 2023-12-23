package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.runtime.TypeVariable;

import java.util.Set;

public class TypeVariableImpl extends Graph implements TypeVariable {

    private final TypeName typeName;

    public TypeVariableImpl(TypeName typeName) {
        this.typeName = typeName;
    }

    @Override
    public String getName() {
        return typeName.getTypeVariable();
    }

    @Override
    public TypeName getTypeName() {
        return typeName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TypeVariableImpl that = (TypeVariableImpl) o;

        return typeName.equals(that.typeName);
    }

    @Override
    public int hashCode() {
        return typeName.hashCode();
    }

    @Override
    protected String toStringImpl(Set<Graph> stack) {
        return typeName.toString();
    }
}
