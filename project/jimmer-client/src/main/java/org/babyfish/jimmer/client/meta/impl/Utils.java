package org.babyfish.jimmer.client.meta.impl;

import org.babyfish.jimmer.client.meta.Type;
import org.jetbrains.annotations.Nullable;

import javax.validation.constraints.Null;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;

class Utils {

    private static final String SPRING_NULLABLE = "org.springframework.lang.Nullable";

    private Utils() {}

    public static Type wrap(Type type, AnnotatedElement annotatedElement) {
        if (annotatedElement.isAnnotationPresent(Nullable.class)) {
            return NullableTypeImpl.of(type);
        }
        if (annotatedElement.isAnnotationPresent(Null.class)) {
            return NullableTypeImpl.of(type);
        }
        if (Arrays.stream(annotatedElement.getAnnotations()).anyMatch(it -> it.annotationType().getName().equals(SPRING_NULLABLE))) {
            return NullableTypeImpl.of(type);
        }
        return type;
    }
}
