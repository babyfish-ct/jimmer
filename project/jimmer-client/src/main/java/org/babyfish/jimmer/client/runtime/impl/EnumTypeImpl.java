package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.runtime.EnumType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class EnumTypeImpl extends Graph implements EnumType {

    private final Class<?> javaType;

    private final List<String> constants;

    public EnumTypeImpl(Class<?> javaType) {
        this.javaType = javaType;
        List<String> constants = new ArrayList<>();
        for (Object constant : javaType.getEnumConstants()) {
            constants.add(((Enum<?>)constant).name());
        }
        this.constants = Collections.unmodifiableList(constants);
    }

    @Override
    public Class<?> getJavaType() {
        return javaType;
    }

    @Override
    public List<String> getConstants() {
        return constants;
    }

    @Override
    protected String toStringImpl(Set<Graph> stack) {
        return javaType.getName();
    }
}
