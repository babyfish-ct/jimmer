package org.babyfish.jimmer.client.meta.impl;

import kotlin.reflect.KTypeParameter;
import org.babyfish.jimmer.client.meta.Type;
import org.babyfish.jimmer.client.meta.Visitor;

public class UnresolvedTypeParameterImpl implements Type {

    private final KTypeParameter typeParameter;

    public UnresolvedTypeParameterImpl(KTypeParameter typeParameter) {
        this.typeParameter = typeParameter;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visitUnresolvedTypeParameter(this);
    }

    @Override
    public boolean hasDefinition() {
        return false;
    }
}
