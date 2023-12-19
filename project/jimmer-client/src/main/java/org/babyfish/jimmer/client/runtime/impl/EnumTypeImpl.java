package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.meta.Doc;
import org.babyfish.jimmer.client.meta.EnumConstant;
import org.babyfish.jimmer.client.meta.TypeDefinition;
import org.babyfish.jimmer.client.runtime.EnumType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class EnumTypeImpl extends Graph implements EnumType {

    private final Class<?> javaType;

    private final List<Constant> constants;

    private final Doc doc;

    public EnumTypeImpl(Class<?> javaType, TypeDefinition definition) {
        this.javaType = javaType;
        List<Constant> constants = new ArrayList<>();
        for (EnumConstant constant : definition.getEnumConstantMap().values()) {
            constants.add(new Constant(constant.getName(), constant.getDoc()));
        }
        this.constants = Collections.unmodifiableList(constants);
        this.doc = definition.getDoc();
    }

    @Override
    public Class<?> getJavaType() {
        return javaType;
    }

    @Override
    public List<Constant> getConstants() {
        return constants;
    }

    @Nullable
    @Override
    public Doc getDoc() {
        return doc;
    }

    @Override
    protected String toStringImpl(Set<Graph> stack) {
        return javaType.getName();
    }
}
