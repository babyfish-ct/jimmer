package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Type;

import javax.validation.constraints.Null;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class Utils {

    private static final String SPRING_NULLABLE = "org.springframework.lang.Nullable";

    private static final Set<Class<?>> BOX_TYPES = new HashSet<>(
            Arrays.asList(
                    Boolean.class,
                    Character.class,
                    Byte.class,
                    Short.class,
                    Integer.class,
                    Long.class,
                    Float.class,
                    Double.class
            )
    );

    private Utils() {}

    public static Type wrap(Context ctx, Type type, AnnotatedElement annotatedElement) {
        if (annotatedElement.isAnnotationPresent(Null.class)) {
            return NullableTypeImpl.of(type);
        }
        if (Arrays.stream(annotatedElement.getAnnotations()).anyMatch(it -> it.annotationType().getName().equals(SPRING_NULLABLE))) {
            return NullableTypeImpl.of(type);
        }
        if (annotatedElement instanceof Field) {
            Field field = (Field) annotatedElement;
            if (field.getType().isPrimitive()) {
                return type;
            }
            if (BOX_TYPES.contains(field.getType())) {
                return NullableTypeImpl.of(type);
            }
            JetBrainsNullity nullity = ctx.getJetBrainsNullity(field.getDeclaringClass());
            if (nullity.isNull(field.getName())) {
                return NullableTypeImpl.of(type);
            }
        } else if (annotatedElement instanceof Method) {
            Method method = (Method) annotatedElement;
            if (method.getReturnType().isPrimitive()) {
                return type;
            }
            if (BOX_TYPES.contains(method.getReturnType())) {
                return NullableTypeImpl.of(type);
            }
            JetBrainsNullity nullity = ctx.getJetBrainsNullity(method.getDeclaringClass());
            if (nullity.isNull(method.getName())) {
                return NullableTypeImpl.of(type);
            }
        } else if (annotatedElement instanceof Parameter) {
            Parameter parameter = (Parameter) annotatedElement;
            if (parameter.getType().isPrimitive()) {
                return type;
            }
            if (BOX_TYPES.contains(parameter.getType())) {
                return NullableTypeImpl.of(type);
            }
            Method method = (Method) parameter.getDeclaringExecutable();
            Parameter[] parameters = method.getParameters();
            int index = 0;
            while (index < parameters.length) {
                if (parameters[index] == parameter) {
                    JetBrainsNullity nullity = ctx.getJetBrainsNullity(method.getDeclaringClass());
                    if (nullity.isNull(method.getName(), index)) {
                        return NullableTypeImpl.of(type);
                    }
                    break;
                }
                index++;
            }
        }
        return type;
    }
}
