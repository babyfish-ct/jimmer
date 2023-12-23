package org.babyfish.jimmer.client.runtime.impl;

import org.babyfish.jimmer.client.meta.TypeName;
import org.babyfish.jimmer.client.runtime.SimpleType;

import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class SimpleTypeImpl extends Graph implements SimpleType {

    private static final Map<TypeName, SimpleType> MAP = new WeakHashMap<>();

    private final Class<?> javaType;

    private SimpleTypeImpl(Class<?> javaType) {
        this.javaType = javaType;
    }

    @Override
    public Class<?> getJavaType() {
        return javaType;
    }

    public static SimpleType of(TypeName typeName) {
        return MAP.computeIfAbsent(typeName, it -> {
            if (typeName.isGenerationRequired()) {
                throw new IllegalArgumentException("The type name \"" + typeName + "\" is not simple type");
            }
            switch (typeName.toString()) {
                case "boolean":
                    return new SimpleTypeImpl(boolean.class);
                case "char":
                    return new SimpleTypeImpl(char.class);
                case "byte":
                    return new SimpleTypeImpl(byte.class);
                case "short":
                    return new SimpleTypeImpl(short.class);
                case "int":
                    return new SimpleTypeImpl(int.class);
                case "long":
                    return new SimpleTypeImpl(long.class);
                case "float":
                    return new SimpleTypeImpl(float.class);
                case "double":
                    return new SimpleTypeImpl(double.class);
            }
            Class<?> javaType;
            try {
                javaType = Class.forName(typeName.toString());
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException("No java type \"" + typeName + "\"", ex);
            }
            return new SimpleTypeImpl(javaType);
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SimpleTypeImpl that = (SimpleTypeImpl) o;

        return javaType.equals(that.javaType);
    }

    @Override
    public int hashCode() {
        return javaType.hashCode();
    }

    @Override
    protected String toStringImpl(Set<Graph> stack) {
        return javaType.getName();
    }
}
