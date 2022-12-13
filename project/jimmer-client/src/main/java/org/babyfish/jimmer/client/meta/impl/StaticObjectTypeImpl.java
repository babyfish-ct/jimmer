package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.ExportFields;
import org.babyfish.jimmer.client.IllegalDocMetaException;
import org.babyfish.jimmer.client.meta.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class StaticObjectTypeImpl implements StaticObjectType {

    private final Class<?> javaType;

    private Map<String, Property> props;

    StaticObjectTypeImpl(Class<?> javaType) {
        this.javaType = javaType;
    }

    @Override
    public Class<?> getJavaType() {
        return javaType;
    }

    @Override
    public boolean isEntity() {
        return false;
    }

    @Override
    public Map<String, Property> getProperties() {
        return props;
    }

    @Override
    public void accept(Visitor visitor) {
        if (visitor.isTypeVisitable(this)) {
            visitor.visitStaticObjectType(this);
            for (Property prop : props.values()) {
                prop.getType().accept(visitor);
            }
        }
    }

    @Override
    public String toString() {
        return "@static:" + javaType.getSimpleName();
    }

    static ObjectType create(Context ctx, Class<?> javaType) {

        StaticObjectTypeImpl impl = new StaticObjectTypeImpl(javaType);
        ctx.addStaticObjectType(impl);

        Map<String, Property> props = new LinkedHashMap<>();
        if (javaType.isAnnotationPresent(ExportFields.class)) {
            while (javaType != Object.class) {
                for (Field field : javaType.getDeclaredFields()) {
                    Type type = ctx.parseType(field.getAnnotatedType());
                    type = Utils.wrap(type, field);
                    props.putIfAbsent(field.getName(), new PropertyImpl(field.getName(), type));
                }
                javaType = javaType.getSuperclass();
            }
        } else {
            while (javaType != Object.class) {
                for (Method method : javaType.getDeclaredMethods()) {
                    if (method.getParameters().length != 0 || method.getReturnType() == void.class) {
                        continue;
                    }
                    String prefix = null;
                    if (method.getReturnType() == boolean.class && method.getName().startsWith("is")) {
                        prefix = "is";
                    } else if (method.getName().startsWith("get")) {
                        prefix = "get";
                    }
                    if (prefix == null) {
                        continue;
                    }
                    if (method.getTypeParameters().length != 0) {
                        throw new IllegalDocMetaException(
                                "Illegal getter method \"" +
                                        method +
                                        "\", it can not have type parameters"
                        );
                    }
                    String name = method.getName().substring(prefix.length());
                    if (name.isEmpty()) {
                        continue;
                    }
                    name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
                    Type type = ctx.parseType(method.getAnnotatedReturnType());
                    type = Utils.wrap(type, method);
                    props.put(name, new PropertyImpl(name, type));
                }
                javaType = javaType.getSuperclass();
            }
        }
        impl.props = Collections.unmodifiableMap(props);
        return impl;
    }
}
