package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.EnumType;
import org.babyfish.jimmer.client.meta.Visitor;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class EnumTypeImpl implements EnumType {

    private final Class<?> javaType;

    private List<String> items;

    public EnumTypeImpl(Class<?> javaType) {
        this.javaType = javaType;

        Method method;
        try {
            method = javaType.getMethod("values");
        } catch (NoSuchMethodException ex) {
            throw new AssertionError("Internal bug", ex);
        }
        Enum<?>[] values;
        try {
            values = (Enum<?>[]) method.invoke(null);
        } catch (IllegalAccessException ex) {
            throw new AssertionError("Internal bug", ex);
        } catch (InvocationTargetException ex) {
            throw new AssertionError("Internal bug", ex.getTargetException());
        }
        this.items = Collections.unmodifiableList(
                Arrays.stream(values).map(Enum::name).collect(Collectors.toList())
        );
    }

    @Override
    public Class<?> getJavaType() {
        return javaType;
    }

    @Override
    public List<String> getItems() {
        return items;
    }

    @Override
    public void accept(Visitor visitor) {
        if (visitor.isTypeVisitable(this)) {
            visitor.visitEnumType(this);
        }
    }

    @Override
    public String toString() {
        return items.stream().collect(Collectors.joining(" | "));
    }
}
